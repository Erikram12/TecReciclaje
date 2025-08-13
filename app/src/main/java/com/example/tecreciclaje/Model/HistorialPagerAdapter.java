package com.example.tecreciclaje.Model;

import android.support.annotation.NonNull;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.tecreciclaje.fragment.CanjeadosFragment;
import com.example.tecreciclaje.fragment.GanadosFragment;

public class HistorialPagerAdapter extends FragmentStateAdapter {
    public HistorialPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (position == 0) ? new GanadosFragment() : new CanjeadosFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
