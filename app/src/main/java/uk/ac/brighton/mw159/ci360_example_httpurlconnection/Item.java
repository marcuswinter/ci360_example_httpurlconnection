package uk.ac.brighton.mw159.ci360_example_httpurlconnection;

import android.graphics.Bitmap;

public class Item {

    public String artist;
    public String date;
    public String image_url;
    public Bitmap image_thumb;

    public Item(String artist, String description, String image_id) {

        this.artist = (artist.length() > 0) ? artist : "Unknown";
        this.date = (description.length() > 0) ? description : "No date available";
        this.image_url = "http://media.vam.ac.uk/media/thira/collection_images/"
                       + image_id.substring(0, 6)
                       + "/"
                       + image_id
                       + "_jpg_w.jpg";
    }
}
