package at.ac.szybbs.bambiguard.fragments.helpertutorial;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import at.ac.szybbs.bambiguard.R;

public class HelperTutorial1Fragment extends Fragment {

    public HelperTutorial1Fragment() {
    }

    public static HelperTutorial1Fragment newInstance() {
        HelperTutorial1Fragment fragment = new HelperTutorial1Fragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_helper_tutorial1, container, false);
    }
}