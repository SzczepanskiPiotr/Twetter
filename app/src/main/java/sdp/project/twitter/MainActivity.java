package sdp.project.twitter;

import android.app.SearchManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import java.io.UnsupportedEncodingException;

import sdp.project.tweeter.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SaveSettings saveSettings = new SaveSettings(getApplicationContext());
        saveSettings.LoadData();
    }

    SearchView searchView;
    Menu myMenu;
    String Query;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        myMenu=menu;
        // searchView code
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (android.widget.SearchView) menu.findItem(R.id.searchbar).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //final Context co=this;
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast.makeText(co, query, Toast.LENGTH_LONG).show();
                Query = null;
                try {
                    //for space with name
                    Query = java.net.URLEncoder.encode(query , "UTF-8");
                } catch (UnsupportedEncodingException e) {

                }
                //loading tweets
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        //   searchView.setOnCloseListener(this);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.home: {
                //loading tweets
                return true;
            }
            case R.id.logout:{
                SaveSettings saveSettings = new SaveSettings(getApplicationContext());
                saveSettings.ClearData();
                return  true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
