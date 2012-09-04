package orc.android

import orc.input.examples.ExamplesSectionFragment
import orc.input.explorer.InputSectionFragment

import android.app.ActionBar
import android.app.FragmentTransaction
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * @author Joao Barbosa, Ricardo Bernardino
 */

class LoadOrcActivity extends FragmentActivity with ActionBar.TabListener {

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
   * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
   * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
   * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  var mSectionsPagerAdapter: SectionsPagerAdapter = null

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  var mViewPager: ViewPager = null

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_load_orc)
    // Create the adapter that will return a fragment for each of the three primary sections
    // of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager())

    // Set up the action bar.
    var actionBar: ActionBar = getActionBar()
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS)

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.pager).asInstanceOf[ViewPager]
    mViewPager.setAdapter(mSectionsPagerAdapter)

    // When swiping between different sections, select the corresponding tab.
    // We can also use ActionBar.Tab#select() to do this if we have a reference to the
    // Tab.
    implicit def viewPagerOnPageSelected(func: (Int) => Unit) = {
      new ViewPager.SimpleOnPageChangeListener() {
        override def onPageSelected(position: Int) = func(position)
      }
    }

    def FuncToViewPager(position: Int) = {
      actionBar.setSelectedNavigationItem(position)
    }

    mViewPager.setOnPageChangeListener(FuncToViewPager _)

    var i: Int = 0
    // For each of the sections in the app, add a tab to the action bar.
    for (i <- 0 until mSectionsPagerAdapter.getCount()) {
      // Create a tab with text corresponding to the page title defined by the adapter.
      // Also specify this Activity object, which implements the TabListener interface, as the
      // listener for when this tab is selected.
      actionBar.addTab(
        actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(i))
          .setTabListener(this))
    }

  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater().inflate(R.menu.activity_load_orc, menu)
    true
  }

  override def onTabUnselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) = {
  }

  override def onTabSelected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) = {
    // When the given tab is selected, switch to the corresponding page in the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition())
    if (tab.getPosition() == 0) {

    }
  }

  override def onTabReselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) = {
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
   * sections of the app.
   */
  class SectionsPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) {

    override def getItem(i: Int): Fragment = {
      var fragment: Fragment = null

      if (i == 1) {
        fragment = new ExamplesSectionFragment()
      } else {
        fragment = new InputSectionFragment()
      }

      var args: Bundle = new Bundle()

      fragment.setArguments(args)

      fragment
    }

    override def getCount(): Int = {
      2
    }

    override def getPageTitle(position: Int): CharSequence = {
      position match {
        case 0 => getString(R.string.title_section1).toUpperCase()
        case 1 => getString(R.string.title_section2).toUpperCase()
        case _ => null
      }
    }
  }

}
