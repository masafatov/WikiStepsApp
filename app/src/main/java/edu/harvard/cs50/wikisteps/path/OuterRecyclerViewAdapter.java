package edu.harvard.cs50.wikisteps.path;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import edu.harvard.cs50.wikisteps.R;

public class OuterRecyclerViewAdapter extends RecyclerView.Adapter<OuterRecyclerViewAdapter.ViewHolder> {

    ArrayList<Path> listPath;
    Context context;

    // An object of RecyclerView.RecycledViewPool is created to share the Views
    // between the outer and the inner RecyclerViews
    private RecyclerView.RecycledViewPool
            viewPool = new RecyclerView.RecycledViewPool();

    public OuterRecyclerViewAdapter(ArrayList<Path> listPath, Context context) {
        this.listPath = listPath;
        this.context = context;
    }

    @NonNull
    @Override
    public OuterRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.outer_list_paths,
                        parent, false);

        // taking the layout and associating in with this holder
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull OuterRecyclerViewAdapter.ViewHolder holder, int position) {
        Path path = listPath.get(position);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                holder.innerRecyclerview.getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);

        layoutManager.setInitialPrefetchItemCount(
                path.getStepList().size());

        InnerRecyclerViewAdapter innerRecyclerViewAdapter =
                new InnerRecyclerViewAdapter(path.getStepList());
        holder.innerRecyclerview.setLayoutManager(layoutManager);
        holder.innerRecyclerview.setAdapter(innerRecyclerViewAdapter);
        holder.innerRecyclerview.setRecycledViewPool(viewPool);
    }

    @Override
    public int getItemCount() {
        return listPath.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView pathTitle;
        private RecyclerView innerRecyclerview;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            innerRecyclerview = itemView.findViewById(R.id.inner_recyclerview);
        }
    }
}
