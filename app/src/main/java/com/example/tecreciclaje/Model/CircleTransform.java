package com.example.tecreciclaje.Model;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

public class CircleTransform implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        // Crear un bitmap cuadrado para la transformación
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        // Dibujar un círculo usando Canvas y Paint
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        // Solo reciclar si se crea un nuevo bitmap
        if (result != source) {
            source.recycle();
        }

        return result;
    }

    @Override
    public String key() {
        return "circle";
    }

}
