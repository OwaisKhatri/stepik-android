package org.stepic.droid.view.activities;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.stepic.droid.R;
import org.stepic.droid.base.FragmentActivityBase;
import org.stepic.droid.concurrency.UpdateCourseTask;
import org.stepic.droid.events.instructors.FailureLoadInstructorsEvent;
import org.stepic.droid.events.instructors.OnResponseLoadingInstructorsEvent;
import org.stepic.droid.events.instructors.StartLoadingInstructorsEvent;
import org.stepic.droid.events.joining_course.FailJoinEvent;
import org.stepic.droid.events.joining_course.SuccessJoinEvent;
import org.stepic.droid.model.Course;
import org.stepic.droid.model.User;
import org.stepic.droid.store.operations.DatabaseManager;
import org.stepic.droid.util.AppConstants;
import org.stepic.droid.util.HtmlHelper;
import org.stepic.droid.util.ProgressHelper;
import org.stepic.droid.view.adapters.InstructorAdapter;
import org.stepic.droid.view.layout_managers.WrapContentLinearLayoutManager;
import org.stepic.droid.web.UserStepicResponse;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class CourseDetailActivity extends FragmentActivityBase {


    private static final String TAG = "unrolled_course";

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.intro_video)
    WebView mIntroView;

    @Bind(R.id.description)
    TextView mDescriptionView;

    @Bind(R.id.course_name)
    TextView mCourseNameView;

    @Bind(R.id.instructors_carousel)
    RecyclerView mInstructorsCarousel;

    @Bind(R.id.load_progressbar)
    ProgressBar mInstructorsProgressBar;

    @Bind(R.id.summary)
    TextView mSummaryView;

    @Bind(R.id.requirements)
    TextView mRequirementsView;

    @Bind(R.id.join_course_layout)
    View mJoinCourseView;

    @Bind(R.id.join_course_spinner)
    ProgressBar mJoinCourseSpinner;

    @BindString(R.string.join_course_impossible)
    String joinCourseImpossible;

    @BindString(R.string.join_course_exception)
    String joinCourseException;

    @BindString(R.string.join_course_web_exception)
    String joinCourseWebException;


    private Course mCourse;
    private List<User> mUserList;
    private InstructorAdapter mInstructorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_not_enrolled_course_detail);
        ButterKnife.bind(this);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        mIntroView.getLayoutParams().width = width;
        mIntroView.getLayoutParams().height = (9 * width) / 16;

        overridePendingTransition(R.anim.slide_in_from_end, R.anim.slide_out_to_start);
        hideSoftKeypad();


        mCourse = (Course) (getIntent().getExtras().get(AppConstants.KEY_COURSE_BUNDLE));

    }

    @Override
    protected void onStart() {
        super.onStart();
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String urlToVideo = mCourse.getIntro();
        Log.i("vimeo", urlToVideo);
        if (urlToVideo == null || urlToVideo.equals("")) {
            mIntroView.setVisibility(View.GONE);
        } else {
            WebSettings webSettings = mIntroView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAppCacheEnabled(true);
            webSettings.setDomStorageEnabled(true);


            final String mimeType = "text/html";
            final String encoding = "UTF-8";
            mIntroView.loadUrl(urlToVideo);
//            mIntroView.loadDataWithBaseURL("", html, mimeType, encoding, "");
//            mHeaderWv.setText(HtmlHelper.fromHtml(mStep.getBlock().getText()));
            mIntroView.setVisibility(View.VISIBLE);
        }


//        String urltovideo = "https://player.vimeo.com/external/111345189.hd.mp4?s=ea9aab1c15434d7bfd3515afaf70a9de&profile_id=113&oauth2_token_id=3605157";
//        MediaController mediaController = new MediaController(this);
//        mediaController.setAnchorView(mIntroView);
//        mIntroView.setMediaController(mediaController);
//        mIntroView.setVideoURI(Uri.parse(urltovideo));
//        mIntroView.start();

        mCourseNameView.setText(mCourse.getTitle());
        mDescriptionView.setText(HtmlHelper.fromHtml(mCourse.getDescription()));
        mSummaryView.setText(HtmlHelper.fromHtml(mCourse.getSummary()));
        mRequirementsView.setText(HtmlHelper.fromHtml(mCourse.getRequirements()));
        if (mCourse.getEnrollment() != 0) {
            mJoinCourseView.setVisibility(View.GONE);
        } else {
            mJoinCourseView.setVisibility(View.VISIBLE);
            mJoinCourseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    joinCourse();
                }
            });
        }

        mUserList = new ArrayList<>();
        mInstructorAdapter = new InstructorAdapter(mUserList, this);
        mInstructorsCarousel.setAdapter(mInstructorAdapter);

        RecyclerView.LayoutManager layoutManager =
                new WrapContentLinearLayoutManager(this,
                        LinearLayoutManager.HORIZONTAL, false);//// TODO: 30.09.15 determine right-to-left-mode
        mInstructorsCarousel.setLayoutManager(layoutManager);


        if (mCourse.getInstructors() != null && mCourse.getInstructors().length != 0) {
            bus.post(new StartLoadingInstructorsEvent(mCourse));
            mShell.getApi().getUsers(mCourse.getInstructors()).enqueue(new Callback<UserStepicResponse>() {
                @Override
                public void onResponse(Response<UserStepicResponse> response, Retrofit retrofit) {
                    bus.post(new OnResponseLoadingInstructorsEvent(mCourse, response, retrofit));
                }

                @Override
                public void onFailure(Throwable t) {
                    bus.post(new FailureLoadInstructorsEvent(mCourse, t));
                }
            });
        }
        bus.register(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Subscribe
    public void onStartLoadingInstructors(StartLoadingInstructorsEvent e) {
        if (e.getCourse() != null && mCourse != null & e.getCourse().getCourseId() == mCourse.getCourseId()) {
            ProgressHelper.activate(mInstructorsProgressBar);
        }
    }

    @Subscribe
    public void onResponseLoadingInstructors(OnResponseLoadingInstructorsEvent e) {
        if (e.getCourse() != null && mCourse != null & e.getCourse().getCourseId() == mCourse.getCourseId()) {

            List<User> users = e.getResponse().body().getUsers();

            mUserList.clear();
            mUserList.addAll(users);
            mInstructorAdapter.notifyDataSetChanged();
            ProgressHelper.dismiss(mInstructorsProgressBar);
        }
    }

    @Subscribe
    public void onFinishLoading(FailureLoadInstructorsEvent e) {
        if (e.getCourse() != null && mCourse != null & e.getCourse().getCourseId() == mCourse.getCourseId()) {
            ProgressHelper.dismiss(mInstructorsProgressBar);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mIntroView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        mInstructorAdapter = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCourse = null;
    }

    @Override
    public void finish() {
        Log.i("result", "finish course activity");
        Intent intent = new Intent();
        intent.putExtra(AppConstants.COURSE_ID_KEY, (Parcelable) mCourse);
        intent.putExtra(AppConstants.ENROLLMENT_KEY, mCourse.getEnrollment());
        setResult(RESULT_OK, intent);
        super.finish();
        overridePendingTransition(R.anim.slide_in_from_start, R.anim.slide_out_to_end);
    }


    private void joinCourse() {
        mJoinCourseView.setEnabled(false);
        ProgressHelper.activate(mJoinCourseSpinner);
        mShell.getApi().tryJoinCourse(mCourse).enqueue(new Callback<Void>() {

            private final Course localCopy = mCourse;

            @Override
            public void onResponse(Response<Void> response, Retrofit retrofit) {
                if (response.isSuccess()) {

                    localCopy.setEnrollment((int) localCopy.getCourseId());

                    UpdateCourseTask updateCourseTask = new UpdateCourseTask(DatabaseManager.Table.enrolled, localCopy);
                    updateCourseTask.execute();

                    UpdateCourseTask updateCourseFeaturedTask = new UpdateCourseTask(DatabaseManager.Table.featured, localCopy);
                    updateCourseFeaturedTask.execute();


                    bus.post(new SuccessJoinEvent(localCopy));

                } else {
                    bus.post(new FailJoinEvent(response));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                bus.post(new FailJoinEvent());
            }
        });
    }

    @Subscribe
    public void onSuccessJoin(SuccessJoinEvent e) {
        e.getCourse().setEnrollment((int) e.getCourse().getCourseId());
        mShell.getScreenProvider().showSections(CourseDetailActivity.this, mCourse);
        finish();
        ProgressHelper.dismiss(mJoinCourseSpinner);
    }

    @Subscribe
    public void onFailJoin(FailJoinEvent e) {
        if (e.getResponse() != null && e.getResponse().code() == 403) {
            Toast.makeText(CourseDetailActivity.this, joinCourseWebException, Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(CourseDetailActivity.this, joinCourseException,
                    Toast.LENGTH_LONG).show();

        }
        ProgressHelper.dismiss(mJoinCourseSpinner);
        mJoinCourseView.setEnabled(true);

    }


}
