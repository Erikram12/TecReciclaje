package com.example.tecreciclaje.Model;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tecreciclaje.R;
import com.example.tecreciclaje.userpanel.MisValesActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.MultiFormatWriter;
import com.squareup.picasso.Picasso;

import android.graphics.Bitmap;

import java.util.List;

public class ValesAdapter extends RecyclerView.Adapter<ValesAdapter.ValeViewHolder> {

    private final List<ValeModel> vales;
    private final Context context;

    public ValesAdapter(List<ValeModel> vales, Context context) {
        this.vales = vales;
        this.context = context;
    }

    @NonNull
    @Override
    public ValeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vale, parent, false);
        return new ValeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ValeViewHolder holder, int position) {
        ValeModel vale = vales.get(position);
        holder.txtProducto.setText(vale.producto);
        holder.txtEstado.setText("Estado: " + vale.estado);

        if (vale.imagen_url != null && !vale.imagen_url.isEmpty()) {
            Picasso.get().load(vale.imagen_url).into(holder.imgVale);
        } else {
            holder.imgVale.setImageResource(R.drawable.cafesito1);
        }

        if (vale.fecha_expiracion > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            String fechaFormateada = sdf.format(new java.util.Date(vale.fecha_expiracion));
            holder.txtExpiracion.setText("Expira el: " + fechaFormateada);
        } else {
            holder.txtExpiracion.setText("Sin fecha de expiración");
        }


        holder.itemView.setOnClickListener(v -> {
            if ("Válido".equalsIgnoreCase(vale.estado)) {
                if (context instanceof MisValesActivity) {
                    // ✅ Ahora usamos el ID real del vale
                    ((MisValesActivity) context).mostrarQrDialog(vale.vale_id);
                }
            } else {
                Toast.makeText(context, "Este vale ya expiró", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return vales.size();
    }

    static class ValeViewHolder extends RecyclerView.ViewHolder {
        TextView txtProducto, txtEstado, txtExpiracion;
        ImageView imgVale;

        public ValeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProducto = itemView.findViewById(R.id.txtProducto);
            txtEstado = itemView.findViewById(R.id.txtEstado);
            txtExpiracion = itemView.findViewById(R.id.txtExpiracion);
            imgVale = itemView.findViewById(R.id.imgVale);
        }
    }
}
