package edu.harvard.cs50.wikisteps.path;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import edu.harvard.cs50.wikisteps.R;

public class InnerRecyclerViewAdapter extends RecyclerView.Adapter<InnerRecyclerViewAdapter.ViewHolder> {

    private ArrayList<Step> stepList;

    public InnerRecyclerViewAdapter(ArrayList<Step> stepList) {
        this.stepList = stepList;
    }

    @NonNull
    @Override
    public InnerRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.inner_list_steps,
                                parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull InnerRecyclerViewAdapter.ViewHolder holder, int position) {
        Step step = stepList.get(position);
        holder.stepTitle.setText(step.getStepTitle());
        holder.currentStep.setTag(step);
    }

    @Override
    public int getItemCount() {
        return stepList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView stepTitle;
        private LinearLayout currentStep;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            stepTitle = itemView.findViewById(R.id.step_title);

            currentStep = itemView.findViewById(R.id.step_card);

            currentStep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Step current = (Step) currentStep.getTag();
                    Log.d("test", current.toString());
                    Intent intent = new Intent(v.getContext(), StepActivity.class);
                    intent.putExtra("title", current.getStepTitle());

                    v.getContext().startActivity(intent);
                }
            });
        }
    }
}
