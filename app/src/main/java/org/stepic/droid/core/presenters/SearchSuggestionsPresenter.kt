package org.stepic.droid.core.presenters

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepic.droid.storage.operations.DatabaseFacade
import org.stepic.droid.ui.adapters.SearchQueriesAdapter
import org.stepic.droid.ui.custom.AutoCompleteSearchView
import org.stepic.droid.web.Api
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SearchSuggestionsPresenter
@Inject constructor(
        private val api: Api,
        private val databaseFacade: DatabaseFacade,
        private val analytic: Analytic,
        @BackgroundScheduler
        private val scheduler: Scheduler,
        @MainScheduler
        private val mainScheduler: Scheduler
        ) : PresenterBase<AutoCompleteSearchView>() {

    companion object {
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 300L
        private const val DB_ELEMENTS_COUNT = 2
    }

    private val compositeDisposable = CompositeDisposable()
    private var searchQueriesAdapter: SearchQueriesAdapter? = null
    private val publisher = PublishSubject.create<String>()

    override fun attachView(view: AutoCompleteSearchView) {
        super.attachView(view)
        initSearchView(view)
    }

    private fun initSearchView(searchView: AutoCompleteSearchView) {
        val compositeDisposable = CompositeDisposable()
        val adapter = searchView.searchQueriesAdapter

        val queryPublisher = publisher
                .debounce(AUTOCOMPLETE_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(scheduler)

        compositeDisposable.add(queryPublisher
                .flatMap { query -> Observable.fromCallable { databaseFacade.getSearchQueries(query, DB_ELEMENTS_COUNT) }.onErrorResumeNext(Observable.empty()) }
                .observeOn(mainScheduler)
                .subscribe({ adapter.rawDBItems = it }, { e -> e.printStackTrace() }))

        compositeDisposable.add(queryPublisher
                .flatMap { query -> api.getSearchQueries(query).toObservable().onErrorResumeNext(Observable.empty()) }
                .observeOn(mainScheduler)
                .subscribe({ adapter.rawAPIItems = it.queries }, { e -> e.printStackTrace() }))
        searchQueriesAdapter = adapter
    }

    fun onQueryTextChange(query: String) {
        searchQueriesAdapter?.constraint = query
        publisher.onNext(query)
    }

    fun onQueryTextSubmit(query: String) {
        analytic.reportEventWithName(Analytic.Search.SEARCH_SUBMITTED, query)
    }

    fun refreshSuggestions() {
        searchQueriesAdapter?.let {
            publisher.onNext(it.constraint)
        }
    }

    override fun detachView(view: AutoCompleteSearchView) {
        compositeDisposable.clear()
        searchQueriesAdapter = null
        super.detachView(view)
    }
}