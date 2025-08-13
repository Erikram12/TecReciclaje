package com.example.tecreciclaje.Model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tecreciclaje.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    private final List<HistorialModel> historialList;

    public HistorialAdapter(List<HistorialModel> historialList) {
        this.historialList = historialList;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        HistorialModel item = historialList.get(position);

        // Cantidad de puntos (con signo)
        int cantidad = item.getCantidad();
        holder.txtCantidad.setText((cantidad > 0 ? "+" : "") + cantidad + " pts");

        // Fecha formateada
        String fechaFormateada = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.getFecha()));
        holder.txtFecha.setText(fechaFormateada);

        // Mostrar producto si es tipo canjeado
        if ("canjeado".equalsIgnoreCase(item.getTipo()) && item.getProducto() != null) {
            holder.txtProducto.setVisibility(View.VISIBLE);
            holder.txtProducto.setText(item.getProducto());
        } else {
            holder.txtProducto.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    public static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView txtCantidad, txtFecha, txtProducto;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCantidad = itemView.findViewById(R.id.txtCantidad);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            txtProducto = itemView.findViewById(R.id.txtProducto);
        }
    }
}
