package com.example.tecreciclaje.userpanel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tecreciclaje.Model.HistorialPagerAdapter
import com.example.tecreciclaje.R
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.utils.TutorialManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HistorialActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: HistorialPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        adapter = HistorialPagerAdapter(this)
        viewPager.adapter = adapter
        setupNavigation()
        setupTabs()

        // Configurar el ViewPager para deshabilitar el swipe horizontal
        viewPager.isUserInputEnabled = false
        
        // MOSTRAR TUTORIAL SI ES NECESARIO
        TutorialManager.showHistorialTutorialIfNeeded(this)
    }

    private fun setupTabs() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if (position == 0) {
                tab.text = "Ganados"
                tab.setIcon(R.drawable.ic_points_tab)
            } else {
                tab.text = "Canjeados"
                tab.setIcon(R.drawable.ic_shopping_cart_tab)
            }
        }.attach()

        // Configurar colores de los iconos
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.icon?.setTint(Color.WHITE)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.icon?.setTint(Color.parseColor("#CCFFFFFF")) // Blanco con transparencia
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No hacer nada
            }
        })
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Selecciona el ítem actual
        bottomNavigationView.selectedItemId = R.id.nav_histori

        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            val currentClass = this::class.java

            when {
                itemId == R.id.nav_home && currentClass != UserPanelDynamic::class.java -> {
                    startActivity(Intent(this, UserPanelDynamic::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_docs && currentClass != MisValesActivity::class.java -> {
                    startActivity(Intent(this, MisValesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_histori && currentClass != HistorialActivity::class.java -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_perfil && currentClass != PerfilActivity::class.java -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> true
            }
        }
    }
}
