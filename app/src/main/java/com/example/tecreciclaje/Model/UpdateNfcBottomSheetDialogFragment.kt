package com.example.tecreciclaje.Model

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.example.tecreciclaje.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UpdateNfcBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_nfc, container, false)
        val btnCancelar = view.findViewById<Button>(R.id.btnCancelar)
        btnCancelar.setOnClickListener {
            nfcAdapter?.disableReaderMode(requireActivity())
            dismiss()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        val act = requireActivity()
        nfcAdapter = NfcAdapter.getDefaultAdapter(act)
        nfcAdapter?.enableReaderMode(
            act, 
            this::onTagDiscovered,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcAdapter?.disableReaderMode(requireActivity())
    }

    private fun onTagDiscovered(tag: Tag) {
        val nfcUid = bytesToHex(tag.id)
        requireActivity().runOnUiThread { actualizarNfc(nfcUid) }
    }

    private fun actualizarNfc(nfcUid: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            context?.let { ctx ->
                Toast.makeText(ctx, "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val db = FirebaseDatabase.getInstance().reference

        // Actualizar el nfcUid del usuario
        db.child("usuarios").child(uid).child("nfcUid").setValue(nfcUid)
            .addOnSuccessListener {
                // Actualizar índice NFC
                db.child("nfc_index").child(nfcUid).setValue(uid)
                context?.let { ctx ->
                    Toast.makeText(ctx, "NFC actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                dismiss()
            }
            .addOnFailureListener { e ->
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error actualizando NFC: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
