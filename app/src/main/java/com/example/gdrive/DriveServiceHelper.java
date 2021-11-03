package com.example.gdrive;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DriveServiceHelper {
    private static final String TAG = "...Drive";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    JSONArray jsonMusic =new JSONArray();
    JSONArray jsonHindiIns =new JSONArray();
    JSONArray jsonEngIns =new JSONArray();
    JSONArray jsonInstruction =new JSONArray();

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }


    public Task<FileList> folder(Context context) {
        return  Tasks.call(mExecutor, () -> {
            String path="/";
            String[] folderIds = {"1XklQXwADPTrXrGw6hcZcQuSSrXhx_tQZ","1jKqr78jIIXmWoh7Lgp_blLuOCsnCgCiA"
                    ,"1yEmalcc3BJranQubDZPkXGoVhhWeC9jY","1KRYsNH_CRO3owS_ePs9WgTbfss2zjA4R"};

            for (String id : folderIds) {
                listFolders(context,id,path);
            }
            return null;
        });
    }


    public void listFolders(Context context, String folderId, String path) {
        Tasks.call(mExecutor, () -> {

            String folderName;
            switch (folderId) {
                case "1XklQXwADPTrXrGw6hcZcQuSSrXhx_tQZ":
                    folderName = "Music";
                    download(path+"/"+ folderName, "", "");
                    listMusicFiles(context, folderId,path+"/" + folderName);
                    break;
                case "1jKqr78jIIXmWoh7Lgp_blLuOCsnCgCiA":
                    folderName = "Hindi Instructions";
                    download(path+"/" +"Instructions"+"/"+ folderName, "", "");
                    listHindiFiles(context, folderId, path+"/"+"Instructions"+"/" + folderName);
                    break;
                case "1yEmalcc3BJranQubDZPkXGoVhhWeC9jY":
                    folderName = "English Instructions";
                    download(path+"/"+"Instructions"+"/"+ folderName, "", "");
                    listEnglishFiles(context, folderId, path +"/"+"Instructions"+"/"+ folderName);
                    break;
                case "1KRYsNH_CRO3owS_ePs9WgTbfss2zjA4R":
                    folderName = "Instructions";
                    download( path+"/" + folderName, "", "");
                    listInstructionFiles(context, folderId, path +"/"+ folderName);
                    break;
            }
            return null;
        });
    }


    public Task<Object> listMusicFiles(Context context, String folderId,String path) {
        return  Tasks.call(mExecutor, () -> {
            String pageToken = null;
            FileList result = null;

            do {
                try {
                    result = mDriveService.files().list()
                            .setQ("mimeType != 'application/vnd.google-apps.folder' and parents in '" + folderId + "' ")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name,size)")
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (result != null) {
                    if(jsonMusic.length() == 0) {
                        for (File file : result.getFiles()) {
                           jsonObj(file,jsonMusic);
                           download(path ,file.getId(),file.getName());
                        }
                    }
                    else{
                        compareExtFiles(context,result,jsonMusic,path);
                    }
                }
                if (result != null) {
                    pageToken = result.getNextPageToken();
                }
            } while (pageToken != null);
            return null;
        });
    }
    public Task<Object> listHindiFiles(Context context, String folderId,String path) {
        return  Tasks.call(mExecutor, () -> {
            String pageToken = null;
            FileList result = null;

            do {
                try {
                    result = mDriveService.files().list()
                            .setQ("mimeType != 'application/vnd.google-apps.folder' and parents in '" + folderId + "' ")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name,size)")
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (result != null) {
                    if(jsonHindiIns.length() == 0) {
                        for (File file : result.getFiles()) {
                            jsonObj(file,jsonHindiIns);
                            download(path ,file.getId(),file.getName());
                        }
                    }
                    else   {
                        Log.i(TAG, "json == "+jsonHindiIns +"\n");
                        compareExtFiles(context,result,jsonHindiIns,path);
                    }
                }
                if (result != null) {
                    pageToken = result.getNextPageToken();
                }
            } while (pageToken != null);
            return null;
        });
    }
    public Task<Object> listEnglishFiles(Context context, String folderId,String path) {
        return  Tasks.call(mExecutor, () -> {
            String pageToken = null;
            FileList result = null;

            do {
                try {
                    result = mDriveService.files().list()
                            .setQ("mimeType != 'application/vnd.google-apps.folder' and parents in '" + folderId + "' ")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name,size)")
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (result != null) {
                    if(jsonEngIns.length() == 0) {
                        for (File file : result.getFiles()) {
                            jsonObj(file,jsonEngIns);
                            download(path ,file.getId(),file.getName());
                        }
                    }
                    else   {
                        compareExtFiles(context,result,jsonEngIns,path);
                    }
                }

                if (result != null) {
                    pageToken = result.getNextPageToken();
                }
            } while (pageToken != null);
            return null;
        });
    }
    public Task<Object> listInstructionFiles(Context context, String folderId,String path) {
        return  Tasks.call(mExecutor, () -> {
            String pageToken = null;
            FileList result = null;
            do {
                try {
                    result = mDriveService.files().list()
                            .setQ("mimeType != 'application/vnd.google-apps.folder' and parents in '" + folderId + "' ")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name,size)")
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (result != null) {
                    if(jsonInstruction.length() == 0) {
                        for (File file : result.getFiles()) {
                            jsonObj(file,jsonInstruction);
                            download(path ,file.getId(),file.getName());
                        }
                    }
                    else   {
                        compareExtFiles(context,result,jsonInstruction,path);
                    }
                }

                if (result != null) {
                    pageToken = result.getNextPageToken();
                }
            } while (pageToken != null);
            return null;
        });
    }

    public void compareExtFiles(Context context, FileList result,JSONArray jsonArray, String path) throws JSONException {
        java.io.File filePath1 = new java.io.File(Environment.getExternalStorageDirectory(), "/AppSounds/" + path);
        java.io.File[] extFiles = filePath1.listFiles();

        if (extFiles != null ) {
            for (java.io.File extFile : extFiles) {
                boolean delete = false;
                String getname = "";

                for (int j = 0; j < result.getFiles().size(); j++) {
                    if (extFile.getName().equals(result.getFiles().get(j).getName())) {
                        delete = true;
                        Log.i(TAG, "not to delete    " + extFiles[j].getName());
                        break;
                    } else if (!extFile.getName().equals(result.getFiles().get(j).getName())) {
                        delete = false;
                        getname = extFile.getName();
                    }
                }
                if (!delete) {
                    Log.i(TAG, "name to delete :" + extFile.getName());
                    delete(path, getname);
                }
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                boolean delete = false;
                for (java.io.File extFile : extFiles) {
                    if (jsonArray.getJSONObject(i).get("name").equals(extFile.getName())) {
                        delete = true;
                        break;
                    } else if (!jsonArray.getJSONObject(i).get("name").equals(extFile.getName())) {
                        delete = false;
                    }
                }
                if (!delete) {
                    Log.i(TAG, "name to delete :" + jsonArray.getJSONObject(i).get("name"));
                    jsonArray.remove(i);
                    --i;
                }
            }
        }
        else {
            jsonArray =null;
        }
        replaceOrAdd(result, jsonArray, path);
    }

    void replaceOrAdd(FileList result,JSONArray jsonArray,String path) throws JSONException {
          if (jsonArray.length() <= 0){
                  for (File file : result.getFiles()) {
                      jsonObj(file,jsonEngIns);
                      download(path ,file.getId(),file.getName());
                  }
              }
          else{
              for (File file : result.getFiles()) {
                  String size = fileSize(file.getSize());
                  String getId = "";
                  String getName = "";
                  boolean b = false;

                  for (int j = 0; j < jsonArray.length(); j++) {
                      if (file.getId().equals(jsonArray.getJSONObject(j).get("id"))
                              || file.getName().equals(jsonArray.getJSONObject(j).get("name"))) {
                          b = true;
                          if (file.getId().equals(jsonArray.getJSONObject(j).get("id"))
                                  && !file.getName().equals(jsonArray.getJSONObject(j).get("name"))) {
                              delete(path, jsonArray.getJSONObject(j).get("name").toString());
                              jsonArray.remove(j);
                              download(path, file.getId(), file.getName());
                              jsonObj(file, jsonArray);
                          }
                          if (!size.equals(jsonArray.getJSONObject(j).get("size"))) {
                              Log.i(TAG, "same obj of json  found, replacing");
                              jsonArray.remove(j);
                              replace(path, file.getId(), file.getName());
                              jsonObj(file, jsonArray);
                          }
                          break;
                      } else if (!file.getId().equals(jsonArray.getJSONObject(j).get("id"))
                              || !file.getName().equals(jsonArray.getJSONObject(j).get("name"))) {
                          b = false;
                          getId = file.getId();
                          getName = file.getName();
                      }
                  }

                  if (!b) {
                      Log.i(TAG, "same obj of json not notFound to be added.");
                      Log.i(TAG, "id :" + getId);
                      Log.i(TAG, "Name :" + getName);
                      jsonObj(file, jsonArray);
                      download(path, getId, getName);
                  }
              }
          }
    }

    public void download( String path, String id, String name) {
        Tasks.call(mExecutor, () -> {

            java.io.File filePath1 = new java.io.File(Environment.getExternalStorageDirectory(), "/AppSounds/"+path+"");
            if (!filePath1.mkdirs()){
                filePath1.mkdirs();
            }
            java.io.File filePath = new java.io.File(Environment.getExternalStorageDirectory(), "/AppSounds/"+path+"/"+name);
            FileOutputStream outputStream = new FileOutputStream(filePath);
            mDriveService.files().get(id)
                    .executeMediaAndDownloadTo(outputStream);
            return null;
        });
    }

    void replace(String path,String id,String name) {
        delete(path,name);
        download(path,id,name);
    }
    void delete(String path,String name){
        java.io.File filePath = new java.io.File(Environment.getExternalStorageDirectory(), "/AppSounds/"+path+"/"+name);
        filePath.delete();
    }

    void jsonObj(File file,JSONArray jsonArray) throws JSONException {
        JSONObject objItem = new JSONObject();
        objItem.put("id", file.getId());
        objItem.put("name", file.getName());
        objItem.put("size",fileSize(file.getSize()));
        jsonArray.put(objItem);

    }

    String fileSize(Long fileSize){
        String size = fileSize + " B";
        if ((fileSize / 1024) < 1024 && (fileSize / 1024) >0) {
            size = (fileSize / 1024) + " KB";
        } else if ((fileSize / 1024) > 1024) {
            size = (fileSize / 1024) / 1024 + " MB";
        }
        return size;
    }
}


