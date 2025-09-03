package com.smartshare.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.smartshare.app.R
import com.smartshare.app.ui.auth.AuthActivity
import com.smartshare.app.ui.files.FilesFragment
import com.smartshare.app.ui.home.DashboardFragment
import com.smartshare.app.ui.transfers.TransfersFragment
import kotlinx.coroutines.launch

/**
 * PUBLIC_INTERFACE
 * MainActivity is the app's primary entry after authentication.
 * It sets up tab-based navigation with:
 *  - Dashboard (AI recommendations, quick actions)
 *  - Files (categories, preview, management)
 *  - Transfers (real-time status)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirect to auth if not signed-in.
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        val fab: FloatingActionButton = findViewById(R.id.fabQuickShare)

        val fragments = listOf(
            DashboardFragment.newInstance(),
            FilesFragment.newInstance(),
            TransfersFragment.newInstance()
        )
        val titles = listOf(
            getString(R.string.tab_dashboard),
            getString(R.string.tab_files),
            getString(R.string.tab_transfers)
        )

        viewPager.adapter = SimplePagerAdapter(this, fragments)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        fab.setOnClickListener {
            // Quick Share: Open file picker and enqueue transfer
            lifecycleScope.launch {
                FilesFragment.startSystemPickerForShare(this@MainActivity)
            }
        }
    }
}
