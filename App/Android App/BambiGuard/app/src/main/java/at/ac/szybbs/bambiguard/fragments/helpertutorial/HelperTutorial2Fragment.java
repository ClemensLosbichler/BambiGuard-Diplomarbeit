package at.ac.szybbs.bambiguard.fragments.helpertutorial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import at.ac.szybbs.bambiguard.R;

public class HelperTutorial2Fragment extends Fragment {

    public HelperTutorial2Fragment() {
    }

    public static HelperTutorial2Fragment newInstance() {
        HelperTutorial2Fragment fragment = new HelperTutorial2Fragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_helper_tutorial2, container, false);
    }
}