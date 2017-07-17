package whiteelephant.com.mapsexample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import whiteelephant.com.mapsexample.models.Directions;

/**
 * Created by prem on 16/07/2017.
 */

public class DirectionsBottomsheetAdapter extends RecyclerView.Adapter<DirectionsBottomsheetAdapter
        .ViewHolder> {


    private List<Directions.Steps> _steps;
    private Context _context;
    private static final String TAG = Utils.getLogTAG(DirectionsBottomsheetAdapter.class);

    public DirectionsBottomsheetAdapter(Context context, List<Directions.Steps> directions) {
        _context = context;
        _steps = directions;
    }


    @Override
    public DirectionsBottomsheetAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.navigations_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DirectionsBottomsheetAdapter.ViewHolder holder, int position) {

        Directions.Steps steps = _steps.get(position);

        holder.__route.setText(Html.fromHtml(steps.htmlInstructions));
        holder.__distance.setText(steps.distance.text);
    }

    @Override
    public int getItemCount() {
        return _steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView __route;
        TextView __distance;

        ViewHolder(View itemView) {
            super(itemView);
            __route = itemView.findViewById(R.id.route);
            __distance = itemView.findViewById(R.id.distance);
        }
    }


    public void swap(List<Directions.Steps> newSteps) {
        _steps = new ArrayList<Directions.Steps>();
        _steps.addAll(newSteps);
        notifyDataSetChanged();
    }
}
