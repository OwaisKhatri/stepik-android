package org.stepic.droid.di.course_list

import dagger.Subcomponent
import org.stepic.droid.base.CoursesDatabaseFragmentBase
import org.stepic.droid.di.tags.TagComponent
import org.stepic.droid.ui.custom.CoursesCarouselView
import org.stepic.droid.ui.fragments.CourseCollectionFragment
import org.stepic.droid.ui.fragments.CourseListFragmentBase
import org.stepic.droid.ui.fragments.CourseSearchFragment
import org.stepic.droid.ui.fragments.FastContinueFragment

@CourseListScope
@Subcomponent(modules = arrayOf(CourseListModule::class))
interface CourseListComponent {

    @Subcomponent.Builder
    interface Builder {
        fun build(): CourseListComponent
    }

    fun tagComponentBuilder(): TagComponent.Builder

    fun inject(fragment: CoursesDatabaseFragmentBase)

    fun inject(fragment: CourseListFragmentBase)

    fun inject(fragment: CourseCollectionFragment)

    fun inject(fragment: CourseSearchFragment)

    fun inject(view: CoursesCarouselView)

    fun inject(fragment: FastContinueFragment)
}
