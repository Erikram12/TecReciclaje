package com.example.tecreciclaje.Model;

public class HistorialModel {
    private int cantidad;
    private long fecha;
    private String tipo;
    private String producto; // solo presente si el tipo es "canjeado"

    // Constructor vacío (requerido por Firebase)
    public HistorialModel() {
    }

    // Constructor completo
    public HistorialModel(int cantidad, long fecha, String tipo, String producto) {
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.tipo = tipo;
        this.producto = producto;
    }

    // Getters y Setters
    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }
}
