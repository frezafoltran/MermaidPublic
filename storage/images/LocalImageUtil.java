package com.foltran.mermaid.storage.images;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.foltran.mermaid.ui.new_trip.EditCardFragment;
import com.foltran.mermaid.ui.profile.ProfileFragment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocalImageUtil {

    private Context context;
    private Uri uri;

    EditCardFragment editCardFragment;
    ProfileFragment profileFragment;

    public LocalImageUtil(EditCardFragment editCardFragment, ProfileFragment profileFragment){
        this.editCardFragment = editCardFragment;
        this.profileFragment = profileFragment;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save a file: path for use with ACTION_VIEW intents
        //currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void selectImage() {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose a way to add the picture");


        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {

                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    //startActivityForResult(takePicture, 0);

                    if (takePicture.resolveActivity(context.getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                        }
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(context,
                                    "com.foltran.android.fileprovider",
                                    photoFile);
                            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            uri = photoURI;
                            //((Activity) context).startActivityForResult(takePicture, 0);
                            if (editCardFragment != null) {
                                editCardFragment.getPictureActivity(takePicture, 0);
                            }
                            else if (profileFragment != null){
                                profileFragment.getPictureActivity(takePicture, 0);
                            }

                            //sample path
                            //content://com.foltran.android.fileprovider/external_files/Pictures/JPEG_20200531_161716_5251264555110498317.jpg
                        }
                    }

                } else if (options[item].equals("Choose from Gallery")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    //((Activity) context).startActivityForResult(pickPhoto , 1);
                    if (editCardFragment != null) {
                        editCardFragment.getPictureActivity(pickPhoto, 1);
                    }
                    else if (profileFragment != null){
                        profileFragment.getPictureActivity(pickPhoto, 1);
                    }

                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    public void setContext(Context context){
        this.context = context;
    }
    public Uri getUri(){
        return uri;
    }


    /**
     * Returns the distance in miles between point 1 and point 2
     * based on their geo coordinates
     *
     * @param lon1 longitude of point 1
     * @param lon2 longitude of point 2
     * @param lat1 latitude of point 1
     * @param lat2 latitude of point 2
     * @return distance between points in km
     */
    static public Double getGeoCoordDist(Double lon1, Double lon2, Double lat1, Double lat2){

        double earthRadius = 6373;

        lat1 = Math.abs(Math.toRadians(lat1));
        lat2 = Math.abs(Math.toRadians(lat2));
        lon1 = Math.abs(Math.toRadians(lon1));
        lon2 = Math.abs(Math.toRadians(lon2));

        Double lonDistance = lon2 - lon1;
        Double latDistance = lat2 - lat1;

        Double a = Math.pow(Math.sin(latDistance/2), 2) +
                (Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(lonDistance/2), 2));

        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c * (1.0/1.6);
    }


}
