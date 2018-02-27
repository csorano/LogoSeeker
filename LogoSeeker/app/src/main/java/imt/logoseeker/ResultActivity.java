package imt.logoseeker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

public class ResultActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Brand brand = (Brand) getIntent().getSerializableExtra("result");
        TextView textres = (TextView) findViewById(R.id.text_result);
        TextView urlres = (TextView) findViewById(R.id.text_Site);

        urlres.setClickable(true);
        urlres.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='" + brand.getUrl() + "'> "+brand.getUrl()+" </a>";
        urlres.setText(Html.fromHtml(text));

        textres.setText(brand.getName());

        // show The Image in a ImageView
        new DownloadImageTask((ImageView) findViewById(R.id.image_result))
                .execute("http://www-rech.telecom-lille.fr/freeorb/train-images/" + brand.getImages()[0]);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
