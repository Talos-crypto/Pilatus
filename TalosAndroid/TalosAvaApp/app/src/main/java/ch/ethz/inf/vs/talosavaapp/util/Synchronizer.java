package ch.ethz.inf.vs.talosavaapp.util;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.talosavaapp.avadata.AvaDataEntry;
import ch.ethz.inf.vs.talosavaapp.avadata.AvaDataImporter;
import ch.ethz.inf.vs.talosavaapp.avadata.AvaOvalDataEntry;
import ch.ethz.inf.vs.talosavaapp.avadata.AvaOvalDataImporter;
import ch.ethz.inf.vs.talosavaapp.talos.TalosAvaSimpleAPI;
import ch.ethz.inf.vs.talosmodule.exceptions.TalosModuleException;
import ch.ethz.inf.vs.talosmodule.main.User;

/**
 * Created by lubu on 03.10.16.
 */

public class Synchronizer {

    private TalosAvaSimpleAPI api;
    private Context context;

    public Synchronizer(Context context, User u) {
        this.context = context;
        api = new TalosAvaSimpleAPI(context, u);
    }

    public void transferDataFromFile(User u, int fileiDd) throws TalosModuleException {
        AvaDataImporter importer = null;
        try {
            importer = new AvaDataImporter(context, fileiDd);
            int count = 0;
            while (importer.hasNext()) {
                api.insertDataset(u, importer.next());
                Log.i("Sync", "Stored line " + count);
            }
        } finally {
            try {
                if(importer!=null)
                    importer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void transferDataFromFile(User u, int fileiDd, int batchsize) throws TalosModuleException {
        AvaDataImporter importer = null;
        List<AvaDataEntry> currentBatch = new ArrayList<>();
        try {
            importer = new AvaDataImporter(context, fileiDd);
            int count = 1;
            while (importer.hasNext()) {
                currentBatch.add(importer.next());

                if(count % batchsize == 0) {
                    api.insertBatchDataset(u, currentBatch);
                    currentBatch = new ArrayList<>();
                    Log.i("Sync", "Stored batch " + count);
                }
                count++;
            }
            if(!currentBatch.isEmpty()) {
                api.insertBatchDataset(u, currentBatch);
                Log.i("Sync", "Stored batch " + count);
            }
        } finally {
            try {
                if(importer!=null)
                    importer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void transferOvaDataFromFile(User u, int fileiDd) throws TalosModuleException {
        AvaOvalDataImporter importer = null;
        try {
            importer = new AvaOvalDataImporter(context, fileiDd);
            int count = 0;
            while (importer.hasNext()) {
                AvaOvalDataEntry entry = importer.next();
                if(entry!=null) {
                    api.insertDatasetOval(u, entry);
                    Log.i("Sync", "Stored line " + count);
                }
            }
        } finally {
            try {
                if(importer!=null)
                    importer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
