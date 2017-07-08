/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.veeradeveloper.videocutter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class FileLoader {



    public static final int MEDIA_DIR_IMAGE = 0;
    public static final int MEDIA_DIR_VIDEO = 2;
    public static final int MEDIA_DIR_DOCUMENT = 3;
    public static final int MEDIA_DIR_CACHE = 4;

    private HashMap<Integer, File> mediaDirs = null;

    private static volatile FileLoader Instance = null;

    public static FileLoader getInstance() {
        FileLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLoader();
                }
            }
        }
        return localInstance;
    }


    public File getDirectory(int type) {
        File dir = mediaDirs.get(type);
        if (dir == null && type != MEDIA_DIR_CACHE) {
            dir = mediaDirs.get(MEDIA_DIR_CACHE);
        }
        try {
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            //don't promt
        }
        return dir;
    }

    public static File getPathToMessage(TLRPC.Message message) {
        if (message == null) {
            return new File("");
        }
        if (message instanceof TLRPC.TL_messageService) {
            if (message.action.photo != null) {
                ArrayList<TLRPC.PhotoSize> sizes = message.action.photo.sizes;
                if (sizes.size() > 0) {
                    TLRPC.PhotoSize sizeFull = getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                    if (sizeFull != null) {
                        return getPathToAttach(sizeFull);
                    }
                }
            }
        } else {
            if (message.media instanceof TLRPC.TL_messageMediaDocument) {
                return getPathToAttach(message.media.document);
            } else if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
                ArrayList<TLRPC.PhotoSize> sizes = message.media.photo.sizes;
                if (sizes.size() > 0) {
                    TLRPC.PhotoSize sizeFull = getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                    if (sizeFull != null) {
                        return getPathToAttach(sizeFull);
                    }
                }
            } else if (message.media instanceof TLRPC.TL_messageMediaWebPage) {
                if (message.media.webpage.document != null) {
                    return getPathToAttach(message.media.webpage.document);
                } else if (message.media.webpage.photo != null) {
                    ArrayList<TLRPC.PhotoSize> sizes = message.media.webpage.photo.sizes;
                    if (sizes.size() > 0) {
                        TLRPC.PhotoSize sizeFull = getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                        if (sizeFull != null) {
                            return getPathToAttach(sizeFull);
                        }
                    }
                }
            }
        }
        return new File("");
    }

    public static File getPathToAttach(TLObject attach) {
        return getPathToAttach(attach, null, false);
    }

    public static File getPathToAttach(TLObject attach, boolean forceCache) {
        return getPathToAttach(attach, null, forceCache);
    }

    public static File getPathToAttach(TLObject attach, String ext, boolean forceCache) {
        File dir = null;
        if (forceCache) {
            dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
        } else {
            if (attach instanceof TLRPC.Document) {
                TLRPC.Document document = (TLRPC.Document) attach;
                if (document.key != null) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    if (MessageObject.isVideoDocument(document)) {
                        dir = getInstance().getDirectory(MEDIA_DIR_VIDEO);
                    } else {
                        dir = getInstance().getDirectory(MEDIA_DIR_DOCUMENT);
                    }
                }
            } else if (attach instanceof TLRPC.PhotoSize) {
                TLRPC.PhotoSize photoSize = (TLRPC.PhotoSize) attach;
                if (photoSize.location == null || photoSize.location.key != null || photoSize.location.volume_id == Integer.MIN_VALUE && photoSize.location.local_id < 0 || photoSize.size < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            } else if (attach instanceof TLRPC.FileLocation) {
                TLRPC.FileLocation fileLocation = (TLRPC.FileLocation) attach;
                if (fileLocation.key != null || fileLocation.volume_id == Integer.MIN_VALUE && fileLocation.local_id < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            }
        }
        if (dir == null) {
            return new File("");
        }
        return new File(dir, getAttachFileName(attach, ext));
    }

    public static TLRPC.PhotoSize getClosestPhotoSizeWithSize(ArrayList<TLRPC.PhotoSize> sizes, int side) {
        return getClosestPhotoSizeWithSize(sizes, side, false);
    }

    public static TLRPC.PhotoSize getClosestPhotoSizeWithSize(ArrayList<TLRPC.PhotoSize> sizes, int side, boolean byMinSide) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        int lastSide = 0;
        TLRPC.PhotoSize closestObject = null;
        for (int a = 0; a < sizes.size(); a++) {
            TLRPC.PhotoSize obj = sizes.get(a);
            if (obj == null) {
                continue;
            }
            if (byMinSide) {
                int currentSide = obj.h >= obj.w ? obj.w : obj.h;
                if (closestObject == null || side > 100 && closestObject.location != null && closestObject.location.dc_id == Integer.MIN_VALUE || obj instanceof TLRPC.TL_photoCachedSize || side > lastSide && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            } else {
                int currentSide = obj.w >= obj.h ? obj.w : obj.h;
                if (closestObject == null || side > 100 && closestObject.location != null && closestObject.location.dc_id == Integer.MIN_VALUE || obj instanceof TLRPC.TL_photoCachedSize || currentSide <= side && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            }
        }
        return closestObject;
    }

    public static String getDocumentFileName(TLRPC.Document document) {
        if (document != null) {
            if (document.file_name != null) {
                return document.file_name;
            }
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute documentAttribute = document.attributes.get(a);
                if (documentAttribute instanceof TLRPC.TL_documentAttributeFilename) {
                    return documentAttribute.file_name;
                }
            }
        }
        return "";
    }

    public static String getAttachFileName(TLObject attach, String ext) {
        if (attach instanceof TLRPC.Document) {
            TLRPC.Document document = (TLRPC.Document) attach;
            String docExt = null;
            if (docExt == null) {
                docExt = getDocumentFileName(document);
                int idx;
                if (docExt == null || (idx = docExt.lastIndexOf('.')) == -1) {
                    docExt = "";
                } else {
                    docExt = docExt.substring(idx);
                }
            }
            if (docExt.length() <= 1) {
                if (document.mime_type != null) {
                    switch (document.mime_type) {
                        case "video/mp4":
                            docExt = ".mp4";
                            break;
                        case "audio/ogg":
                            docExt = ".ogg";
                            break;
                        default:
                            docExt = "";
                            break;
                    }
                } else {
                    docExt = "";
                }
            }
            if (docExt.length() > 1) {
                return document.dc_id + "_" + document.id + docExt;
            } else {
                return document.dc_id + "_" + document.id;
            }
        } else if (attach instanceof TLRPC.PhotoSize) {
            TLRPC.PhotoSize photo = (TLRPC.PhotoSize) attach;
            if (photo.location == null || photo.location instanceof TLRPC.TL_fileLocationUnavailable) {
                return "";
            }
            return photo.location.volume_id + "_" + photo.location.local_id + "." + (ext != null ? ext : "jpg");
        } else if (attach instanceof TLRPC.FileLocation) {
            if (attach instanceof TLRPC.TL_fileLocationUnavailable) {
                return "";
            }
            TLRPC.FileLocation location = (TLRPC.FileLocation) attach;
            return location.volume_id + "_" + location.local_id + "." + (ext != null ? ext : "jpg");
        }
        return "";
    }

}
