package whiteelephant.com.mapsexample;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import whiteelephant.com.mapsexample.models.Directions;


/**
 * A simple {@link Fragment} subclass.
 */
public class DirectionsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView _directionListView;

    public static DirectionsBottomSheet newInstance(Bundle bundle) {

        DirectionsBottomSheet fragment = new DirectionsBottomSheet();
        fragment.setArguments(bundle);
        return fragment;
    }


    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_directions_bottom_sheet, container, false);
        _directionListView = view.findViewById(R.id.navigation_directions_list);
        return view;
    }*/

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        // super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.fragment_directions_bottom_sheet, null);
        _directionListView = contentView.findViewById(R.id.navigation_directions_list);
        _directionListView.setLayoutManager(new LinearLayoutManager(getContext()));
        // _directionListView.setAdapter(new DirectionsBottomsheetAdapter());
        dialog.setContentView(contentView);
    }

}
