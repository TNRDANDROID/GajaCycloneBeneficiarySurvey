package com.nic.gajacyclonebeneficiarysurvey.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.nic.gajacyclonebeneficiarysurvey.R;
import com.nic.gajacyclonebeneficiarysurvey.adapter.PendingAdapter;
import com.nic.gajacyclonebeneficiarysurvey.api.Api;
import com.nic.gajacyclonebeneficiarysurvey.api.ApiService;
import com.nic.gajacyclonebeneficiarysurvey.api.ServerResponse;
import com.nic.gajacyclonebeneficiarysurvey.constant.AppConstant;
import com.nic.gajacyclonebeneficiarysurvey.dataBase.DBHelper;
import com.nic.gajacyclonebeneficiarysurvey.dataBase.dbData;
import com.nic.gajacyclonebeneficiarysurvey.databinding.PendingScreenBinding;
import com.nic.gajacyclonebeneficiarysurvey.model.GajaCycloneBeneficiarySurvey;
import com.nic.gajacyclonebeneficiarysurvey.session.PrefManager;
import com.nic.gajacyclonebeneficiarysurvey.utils.UrlGenerator;
import com.nic.gajacyclonebeneficiarysurvey.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class PendingScreen extends AppCompatActivity implements Api.ServerResponseListener {
    public PendingScreenBinding pendingScreenBinding;
    private RecyclerView recyclerView;
    private PendingAdapter pendingAdapter;
    private PrefManager prefManager;
    public dbData dbData = new dbData(this);
    public static DBHelper dbHelper;
    public static SQLiteDatabase db;
    private Activity context;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingScreenBinding = DataBindingUtil.setContentView(this, R.layout.pending_screen);
        pendingScreenBinding.setActivity(this);
        context = this;
        prefManager = new PrefManager(this);
        try {
            dbHelper = new DBHelper(this);
            db = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recyclerView = pendingScreenBinding.pendingList;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        new fetchPendingtask().execute();
    }



    public class fetchPendingtask extends AsyncTask<Void, Void,
            ArrayList<GajaCycloneBeneficiarySurvey>> {
        @Override
        protected ArrayList<GajaCycloneBeneficiarySurvey> doInBackground(Void... params) {
            dbData.open();
            ArrayList<GajaCycloneBeneficiarySurvey> pmaySurveys = new ArrayList<>();
            pmaySurveys = dbData.getSavedPMAYDetails();
            Log.d("PMAY_COUNT", String.valueOf(pmaySurveys.size()));
            return pmaySurveys;
        }

        @Override
        protected void onPostExecute(ArrayList<GajaCycloneBeneficiarySurvey> pmaySurveys) {
            super.onPostExecute(pmaySurveys);
            recyclerView.setVisibility(View.VISIBLE);
            pendingAdapter = new PendingAdapter(PendingScreen.this, pmaySurveys);
            recyclerView.setAdapter(pendingAdapter);
        }
    }



    public JSONObject savePMAYImagesJsonParams(JSONObject savePMAYDataSet) {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), savePMAYDataSet.toString());
        JSONObject dataSet = new JSONObject();
        try {
            dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
            dataSet.put(AppConstant.DATA_CONTENT, authKey);

            new ApiService(this).makeJSONObjectRequest("savePMAYImages", Api.Method.POST, UrlGenerator.getPMAYListUrl(), dataSet, "not cache", this);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("savePMAYImages", "" + authKey);
        return dataSet;
    }
    @Override
    public void OnMyResponse(ServerResponse serverResponse) {
        try {
            String urlType = serverResponse.getApi();
            JSONObject responseObj = serverResponse.getJsonResponse();

            if ("savePMAYImages".equals(urlType) && responseObj != null) {
                String key = responseObj.getString(AppConstant.ENCODE_DATA);
                String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                    Utils.showAlert(this, "Uploaded");
                    db.delete(DBHelper.SAVE_PMAY_DETAILS,"id = ?",new String[] {prefManager.getKeyDeleteId()});
                    db.delete(DBHelper.SAVE_PMAY_IMAGES, "pmay_id = ? ", new String[]{prefManager.getKeyDeleteId()});
                    new fetchPendingtask().execute();
                    pendingAdapter.notifyDataSetChanged();
                }
                else if(jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("FAIL")){
                    Toasty.error(this, jsonObject.getString("MESSAGE"), Toast.LENGTH_LONG, true).show();
                    db.delete(DBHelper.SAVE_PMAY_DETAILS,"id = ?",new String[] {prefManager.getKeyDeleteId()});
                    db.delete(DBHelper.SAVE_PMAY_IMAGES, "pmay_id = ? ", new String[]{prefManager.getKeyDeleteId()});
                    new fetchPendingtask().execute();
                    pendingAdapter.notifyDataSetChanged();
                }
                Log.d("saved_response", "" + responseDecryptedBlockKey);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void OnError(VolleyError volleyError) {

    }

    public void homePage() {
        Intent intent = new Intent(this, HomePage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Home", "Home");
        startActivity(intent);
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    public void onBackPress() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }
}
