package com.devroid.dropdashwallet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentTransfersAdapter extends RecyclerView.Adapter<RecentTransfersAdapter.RecentTransfersViewHolder> {
    Context context;
    List<RecentTransfer> recentTransferList;

    public interface OnItemClickListener {
        void onItemClick(RecentTransfer transfer);
    }

    OnItemClickListener listener;
    public RecentTransfersAdapter (Context context, List<RecentTransfer> recentTransferList, OnItemClickListener listener) {
        this.context = context;
        this.recentTransferList = recentTransferList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public RecentTransfersAdapter.RecentTransfersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_fund_transfers, parent, false);
        return new RecentTransfersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransfersAdapter.RecentTransfersViewHolder holder, int position) {
        RecentTransfer recentTransfer = recentTransferList.get(position);

        holder.recentTransferIdName.setText(recentTransfer.getName());
        holder.recentTransferAmount.setText(String.valueOf(recentTransfer.getAmount()));

        holder.itemView.setOnClickListener(v -> {
            listener.onItemClick(recentTransfer);
        });
    }

    @Override
    public int getItemCount() {
        return recentTransferList.size();
    }

    static class RecentTransfersViewHolder extends RecyclerView.ViewHolder {
        TextView recentTransferIdName, recentTransferAmount;

        public RecentTransfersViewHolder(@NonNull View itemView) {
            super(itemView);

            recentTransferIdName = itemView.findViewById(R.id.recentTransferIdName);
            recentTransferAmount = itemView.findViewById(R.id.recentTransferAmount);
        }
    }
}
