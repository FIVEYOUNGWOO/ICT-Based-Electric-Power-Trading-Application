package com.borax12.mg_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class myadapter extends RecyclerView.Adapter<myadapter.myviewholder>
{
    ArrayList<model> dataholder;

    public myadapter(ArrayList<model> dataholder) {
        this.dataholder = dataholder;
    }

    @NonNull
    @Override
    public myviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.purchased_1_activity,parent,false);
        return new myviewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myviewholder holder, int position)
    {
        holder.dremain.setText(dataholder.get(position).getRemain());
        holder.dpurchase.setText(dataholder.get(position).getPurchase());
        holder.dpay.setText(dataholder.get(position).getPay());
    }

    @Override
    public int getItemCount() {
        return dataholder.size();
    }

    class myviewholder extends RecyclerView.ViewHolder
    {
        TextView dremain,dpurchase,dpay;

        public myviewholder(@NonNull View itemView)
        {
            super(itemView);
            dremain=(TextView)itemView.findViewById(R.id.displayremain);
            dpurchase=(TextView)itemView.findViewById(R.id.displaypurchase);
            dpay=(TextView)itemView.findViewById(R.id.displaypay);

        }
    }

}
