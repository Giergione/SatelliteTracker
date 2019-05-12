package com.example.satellitetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ErrorFragment extends DialogFragment {

    public interface CancelListener {
        void onCancel();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof CancelListener)) {
            throw new ClassCastException(activity.toString() + " must implement YesNoListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("Request response error")
                .setMessage("possible causes: " +
                        "\n- Issue with internet connection." +
                        "\n- Inserted satellite name is invalid." +
                        "\n- Location data invalid - make sure the application has access to your location info.")
                .setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((CancelListener) getActivity()).onCancel();
                    }
                })
                .create();
    }

}
