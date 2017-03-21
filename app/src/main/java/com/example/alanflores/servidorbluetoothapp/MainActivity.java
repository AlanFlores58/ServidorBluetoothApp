package com.example.alanflores.servidorbluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private Button buttonVisible;
    private TextView textMensajes;
    private static final int PETICION_BLUETOOTH = 0;
    private final UUID MI_UUID = UUID.fromString("6049a354-3df0-11e3-8e7a-ce3f5508acd9");
    private BluetoothServerSocket bluetoothServerSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textMensajes = (TextView)findViewById(R.id.text_mensaje);
        buttonVisible = (Button)findViewById(R.id.button_visible);
        buttonVisible.setOnClickListener(onClickListener);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(MainActivity.this, "No se ha encontrado un dispositivo Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }else if(bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, PETICION_BLUETOOTH);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case PETICION_BLUETOOTH:
                if (resultCode == RESULT_OK){
                    textMensajes.append("\n" + "Estado: Bluetooth habilitado");
                }else if (resultCode == RESULT_CANCELED){
                    textMensajes.append("\n" + "Estado: Bluetooth deshabilitado");
                    finish();
                }
        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!iniciarBluetooth()){
                textMensajes.append("\n" + "Estado: Error al pasar a modo visible");
            }
        }
    };

    class AceptarSolicitudesAsync extends AsyncTask<Integer,String, Void>{
        private BluetoothServerSocket bluetoothServerSocketTemp;
        @Override
        protected Void doInBackground(Integer... integers) {
            try{
                bluetoothServerSocketTemp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("com.example.alanflores.clientebluetoothapp", MI_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            bluetoothServerSocket = bluetoothServerSocketTemp;

            BluetoothSocket bluetoothSocket = null;
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(bluetoothSocket != null){
                CreaerConexionesAsync creaerConexionesAsync = new CreaerConexionesAsync();
                creaerConexionesAsync.execute(bluetoothSocket);
                publishProgress("Estado : Bluetooth clent conectado");
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            textMensajes.append("\n" + values[0]);
        }
    }

    class CreaerConexionesAsync extends AsyncTask<BluetoothSocket, String, Integer>{

        private BluetoothSocket bluetoothSocket;
        private BufferedReader bufferedReader;
        @Override
        protected Integer doInBackground(BluetoothSocket... bluetoothSockets) {
            bluetoothSocket = bluetoothSockets[0];
            try{
                bufferedReader = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true){
                try {
                    String mensaje = bufferedReader.readLine();
                    publishProgress(mensaje);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            textMensajes.append("\n" + values[0]);
        }
    }

    private boolean iniciarBluetooth(){
        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 50);
            startActivity(intent);
            textMensajes.append("\n" + "Estado : modo encontrables");
            AceptarSolicitudesAsync aceptarSolicitudesAsync = new AceptarSolicitudesAsync();
            aceptarSolicitudesAsync.execute(0);
            textMensajes.append("\n" + "Estado : esperando conexiones");
            return true;
        }
        return false;
    }

}
