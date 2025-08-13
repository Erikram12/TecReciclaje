package com.example.tecreciclaje.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.annotation.NonNull;
import android.view.*;
import com.example.tecreciclaje.Model.HistorialAdapter;
import com.example.tecreciclaje.Model.HistorialModel;
import com.example.tecreciclaje.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class CanjeadosFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<HistorialModel> listaHistorial = new ArrayList<>();
    private HistorialAdapter adapter;
    private FirebaseUser user;

    public CanjeadosFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistorialAdapter(listaHistorial);
        recyclerView.setAdapter(adapter);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            cargarHistorial("canjeado");
        }

        return view;
    }

    private void cargarHistorial(String tipo) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("usuarios").child(user.getUid()).child("historial");

        ref.orderByChild("tipo").equalTo(tipo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaHistorial.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            HistorialModel item = s.getValue(HistorialModel.class);
                            if (item != null) listaHistorial.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
