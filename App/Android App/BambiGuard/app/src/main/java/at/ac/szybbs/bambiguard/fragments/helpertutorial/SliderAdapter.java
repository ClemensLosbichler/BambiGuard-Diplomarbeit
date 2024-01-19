package at.ac.szybbs.bambiguard.fragments.helpertutorial;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SliderAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragments;

    public SliderAdapter(ArrayList<Fragment> fragments, FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragments = fragments;
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }
}
