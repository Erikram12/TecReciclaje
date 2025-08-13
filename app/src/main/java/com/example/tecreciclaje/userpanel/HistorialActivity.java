package com.example.tecreciclaje.userpanel;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tecreciclaje.Model.HistorialPagerAdapter;
import com.example.tecreciclaje.R;
import com.example.tecreciclaje.UserPanel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HistorialActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HistorialPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        adapter = new HistorialPagerAdapter(this);
        viewPager.setAdapter(adapter);
        setupNavigation();


        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Ganados");
                tab.setIcon(R.drawable.baseline_edit_24);
            } else {
                tab.setText("Canjeados");
                tab.setIcon(R.drawable.baseline_manage_history_24);
            }
        }).attach();

        // Asegurar que los íconos se actualicen correctamente con colores
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getIcon() != null)
                    tab.getIcon().setTint(Color.WHITE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null)
                    tab.getIcon().setTint(Color.parseColor("#B0D9CE")); // verde pastel apagado
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // No hacer nada
            }
        });
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Selecciona el ítem actual (cambia según la actividad actual)
        bottomNavigationView.setSelectedItemId(R.id.nav_histori); // 👈 actualízalo en cada Activity

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Class<?> currentClass = getClass(); // Detecta clase actual

            if (itemId == R.id.nav_home && !currentClass.equals(UserPanel.class)) {
                startActivity(new Intent(this, UserPanel.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_docs && !currentClass.equals(MisValesActivity.class)) {
                startActivity(new Intent(this, MisValesActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_histori && !currentClass.equals(HistorialActivity.class)) {
                startActivity(new Intent(this, HistorialActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_perfil && !currentClass.equals(PerfilActivity.class)) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return true; // Si ya estás en la actividad, simplemente no hace nada
        });
    }

}
