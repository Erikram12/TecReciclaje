package com.example.tecreciclaje.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.*;
import com.example.tecreciclaje.Model.HistorialAdapter;
import com.example.tecreciclaje.Model.HistorialModel;
import com.example.tecreciclaje.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class GanadosFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<HistorialModel> listaHistorial = new ArrayList<>();
    private HistorialAdapter adapter;
    private FirebaseUser user;

    public GanadosFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistorialAdapter(listaHistorial);
        recyclerView.setAdapter(adapter);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            cargarHistorialGanado(user.getUid());
        }

        return view;
    }

    private void cargarHistorialGanado(String uid) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("usuarios").child(uid).child("ptsganados");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaHistorial.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    HistorialModel model = s.getValue(HistorialModel.class);
                    if (model != null) {
                        listaHistorial.add(0, model); // Agrega al inicio
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Puedes agregar un Toast si deseas mostrar el error
            }
        });
    }
}
