package com.example.tecreciclaje.Model

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tecreciclaje.fragment.CanjeadosFragment
import com.example.tecreciclaje.fragment.GanadosFragment

class HistorialPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) GanadosFragment() else CanjeadosFragment()
    }

    override fun getItemCount(): Int = 2
}
