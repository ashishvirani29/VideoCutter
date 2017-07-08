/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.veeradeveloper.videocutter;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;


import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageObject {

    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;

    public TLRPC.Message messageOwner;
    public CharSequence messageText;
    public MessageObject replyMessageObject;
    public int type = 1000;
    public int contentType;
    public String dateKey;
    public String monthKey;
    public ArrayList<TLRPC.PhotoSize> photoThumbs;
    public VideoEditedInfo videoEditedInfo;
    public boolean attachPathExists;
    public boolean mediaExists;

    private static TextPaint textPaint;

    private boolean layoutCreated;


    public MessageObject(TLRPC.Message message, AbstractMap<Integer, TLRPC.User> users, AbstractMap<Integer, TLRPC.Chat> chats, boolean generateLayout) {
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        }

//        textPaint.setTextSize(AndroidUtilities.dp(MessagesController.getInstance().fontSize));

        messageOwner = message;

        if (message.replyMessage != null) {
            replyMessageObject = new MessageObject(message.replyMessage, users, chats, false);
        }

        TLRPC.User fromUser = null;
        if (message.from_id > 0) {
            if (users != null) {
                fromUser = users.get(message.from_id);
            }

        } else {
            messageText = message.message;
        }
        if (messageText == null) {
            messageText = "";
        }

        setType();

        Calendar rightNow = new GregorianCalendar();
        rightNow.setTimeInMillis((long) (messageOwner.date) * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);
        int dateMonth = rightNow.get(Calendar.MONTH);
        dateKey = String.format("%d_%02d_%02d", dateYear, dateMonth, dateDay);
        monthKey = String.format("%d_%02d", dateYear, dateMonth);

        if (messageOwner.message != null && messageOwner.id < 0 && messageOwner.message.length() > 6 && isVideo()) {
            videoEditedInfo = new VideoEditedInfo();
            videoEditedInfo.parseString(messageOwner.message);
        }

        layoutCreated = generateLayout;
        generateThumbs(false);
        checkMediaExistance();
    }


    public void setType() {
        int oldType = type;
        if (messageOwner instanceof TLRPC.TL_message || messageOwner instanceof TLRPC.TL_messageForwarded_old2) {
            if (isMediaEmpty()) {
                type = 0;
                if (messageText == null || messageText.length() == 0) {
                    messageText = "Empty message";
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                type = 1;
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaGeo || messageOwner.media instanceof TLRPC.TL_messageMediaVenue) {
                type = 4;
            } else if (isVideo()) {
                type = 3;

            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaContact) {
                type = 12;
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaUnsupported) {
                type = 0;
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                if (messageOwner.media.document.mime_type != null) {
                    if (isGifDocument(messageOwner.media.document)) {
                        type = 8;
                    }  else {
                        type = 9;
                    }
                } else {
                    type = 9;
                }
            }
        } else if (messageOwner instanceof TLRPC.TL_messageService) {
            if (messageOwner.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                type = 0;
            } else if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditPhoto || messageOwner.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                contentType = 1;
                type = 11;
            } else if (messageOwner.action instanceof TLRPC.TL_messageEncryptedAction) {
                if (messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionScreenshotMessages || messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
                    contentType = 1;
                    type = 10;
                } else {
                    contentType = -1;
                    type = -1;
                }
            } else {
                contentType = 1;
                type = 10;
            }
        }
        if (oldType != 1000 && oldType != type) {
            generateThumbs(false);
        }
    }


    public static boolean isGifDocument(TLRPC.Document document) {
        return document != null && document.thumb != null && document.mime_type != null && (document.mime_type.equals("image/gif") || isNewGifDocument(document));
    }

    public static boolean isNewGifDocument(TLRPC.Document document) {
        if (document != null && document.mime_type != null && document.mime_type.equals("video/mp4")) {
            for (int a = 0; a < document.attributes.size(); a++) {
                if (document.attributes.get(a) instanceof TLRPC.TL_documentAttributeAnimated) {
                    return true;
                }
            }
        }
        return false;
    }

    public void generateThumbs(boolean update) {
        if (messageOwner instanceof TLRPC.TL_messageService) {
            if (messageOwner.action instanceof TLRPC.TL_messageActionChatEditPhoto) {
                if (!update) {
                    photoThumbs = new ArrayList<>(messageOwner.action.photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                        for (int b = 0; b < messageOwner.action.photo.sizes.size(); b++) {
                            TLRPC.PhotoSize size = messageOwner.action.photo.sizes.get(b);
                            if (size instanceof TLRPC.TL_photoSizeEmpty) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            }
                        }
                    }
                }
            }
        } else if (messageOwner.media != null && !(messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
            if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                if (!update || photoThumbs != null && photoThumbs.size() != messageOwner.media.photo.sizes.size()) {
                    photoThumbs = new ArrayList<>(messageOwner.media.photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                        for (int b = 0; b < messageOwner.media.photo.sizes.size(); b++) {
                            TLRPC.PhotoSize size = messageOwner.media.photo.sizes.get(b);
                            if (size instanceof TLRPC.TL_photoSizeEmpty) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            }
                        }
                    }
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                if (!(messageOwner.media.document.thumb instanceof TLRPC.TL_photoSizeEmpty)) {
                    if (!update) {
                        photoThumbs = new ArrayList<>();
                        photoThumbs.add(messageOwner.media.document.thumb);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty() && messageOwner.media.document.thumb != null) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(0);
                        photoObject.location = messageOwner.media.document.thumb.location;
                        photoObject.w = messageOwner.media.document.thumb.w;
                        photoObject.h = messageOwner.media.document.thumb.h;
                    }
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                if (messageOwner.media.webpage.photo != null) {
                    if (!update || photoThumbs == null) {
                        photoThumbs = new ArrayList<>(messageOwner.media.webpage.photo.sizes);
                    } else if (!photoThumbs.isEmpty()) {
                        for (int a = 0; a < photoThumbs.size(); a++) {
                            TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                            for (int b = 0; b < messageOwner.media.webpage.photo.sizes.size(); b++) {
                                TLRPC.PhotoSize size = messageOwner.media.webpage.photo.sizes.get(b);
                                if (size instanceof TLRPC.TL_photoSizeEmpty) {
                                    continue;
                                }
                                if (size.type.equals(photoObject.type)) {
                                    photoObject.location = size.location;
                                    break;
                                }
                            }
                        }
                    }
                } else if (messageOwner.media.webpage.document != null) {
                    if (!(messageOwner.media.webpage.document.thumb instanceof TLRPC.TL_photoSizeEmpty)) {
                        if (!update) {
                            photoThumbs = new ArrayList<>();
                            photoThumbs.add(messageOwner.media.webpage.document.thumb);
                        } else if (photoThumbs != null && !photoThumbs.isEmpty() && messageOwner.media.webpage.document.thumb != null) {
                            TLRPC.PhotoSize photoObject = photoThumbs.get(0);
                            photoObject.location = messageOwner.media.webpage.document.thumb.location;
                        }
                    }
                }
            }
        }
    }

    public static boolean isVideoDocument(TLRPC.Document document) {
        if (document != null) {
            boolean isAnimated = false;
            boolean isVideo = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    isVideo = true;
                } else if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                    isAnimated = true;
                }
            }
            return isVideo && !isAnimated;
        }
        return false;
    }

    public TLRPC.Document getDocument() {
        if (messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
            return messageOwner.media.webpage.document;
        }
        return messageOwner.media != null ? messageOwner.media.document : null;
    }

    public static boolean isVideoMessage(TLRPC.Message message) {
        if (message.media instanceof TLRPC.TL_messageMediaWebPage) {
            return isVideoDocument(message.media.webpage.document);
        }
        return message.media != null && message.media.document != null && isVideoDocument(message.media.document);
    }

    public boolean isVideo() {
        return isVideoMessage(messageOwner);
    }


    public boolean isMediaEmpty() {
        return isMediaEmpty(messageOwner);
    }

    public static boolean isMediaEmpty(TLRPC.Message message) {
        return message == null || message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty || message.media instanceof TLRPC.TL_messageMediaWebPage;
    }


    public void checkMediaExistance() {
        attachPathExists = false;
        mediaExists = false;
        if (type == 1) {
            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
            if (currentPhotoObject != null) {
                mediaExists = FileLoader.getPathToMessage(messageOwner).exists();
            }
        } else if (type == 8 || type == 3 || type == 9 || type == 2 || type == 14) {
            if (messageOwner.attachPath != null && messageOwner.attachPath.length() > 0) {
                File f = new File(messageOwner.attachPath);
                attachPathExists = f.exists();
            }
            if (!attachPathExists) {
                mediaExists = FileLoader.getPathToMessage(messageOwner).exists();
            }
        } else {
            TLRPC.Document document = getDocument();
            if (document != null) {
                mediaExists = FileLoader.getPathToAttach(document).exists();
            } else if (type == 0) {
                TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
                if (currentPhotoObject == null) {
                    return;
                }
                if (currentPhotoObject != null) {
                    mediaExists = FileLoader.getPathToAttach(currentPhotoObject, true).exists();
                }
            }
        }
    }
}
