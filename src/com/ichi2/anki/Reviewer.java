/****************************************************************************************
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2014 Bruno Romero de Azevedo <brunodea@inf.ufsm.br>                    *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/
// TODO: implement own menu? http://www.codeproject.com/Articles/173121/Android-Menus-My-Way

package com.ichi2.anki;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.ichi2.anki.reviewer.WhiteboardListener;
import com.ichi2.async.DeckTask;
import com.ichi2.libanki.Collection;
import com.ichi2.widget.WidgetStatus;

import org.json.JSONException;

public class Reviewer extends AbstractFlashcardViewer implements WhiteboardListener {
    private boolean mHasDrawerSwipeConflicts = false;
    
    @Override
    protected void setTitle() {
        try {
            String[] title = mSched.getCol().getDecks().current().getString("name").split("::");
            AnkiDroidApp.getCompat().setTitle(this, title[title.length - 1], mInvertedColors);
            super.setTitle(title[title.length - 1]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        AnkiDroidApp.getCompat().setSubtitle(this, "", mInvertedColors);
    }


    @Override
    protected void onCollectionLoaded(Collection col) {
        setWhiteboardListener(this);
        super.onCollectionLoaded(col);
        // Load the first card and start reviewing. Uses the answer card
        // task to load a card, but since we send null
        // as the card to answer, no card will be answered.
        col.getSched().reset();     // Reset schedule incase card had previous been loaded
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mAnswerCardHandler, new DeckTask.TaskData(mSched, null,
                0));

        // Since we aren't actually answering a card, decrement the rep count
        mSched.setReps(mSched.getReps() - 1);
        disableDrawerSwipeOnConflicts();
    }


    @Override
    public void displayCardQuestion() {
        // show timer, if activated in the deck's preferences
        initTimer();
        super.displayCardQuestion();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isFinishing()) {
            if (AnkiDroidApp.colIsOpen()) {
                WidgetStatus.update(this, mSched.progressToday(null, mCurrentCard, true));
            }
        }
        UIUtils.saveCollectionInBackground();
    }


    @Override
    public void onShowWhiteboard() {
        disableDrawerSwipe();
    }


    @Override
    public void onHideWhiteboard() {
        if (!mHasDrawerSwipeConflicts) {
            enableDrawerSwipe();
        }
    }
    
    private void disableDrawerSwipeOnConflicts() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        boolean gesturesEnabled = AnkiDroidApp.initiateGestures(this, preferences);
        if (gesturesEnabled) {
            int gestureSwipeUp = Integer.parseInt(preferences.getString("gestureSwipeUp", "9"));
            int gestureSwipeDown = Integer.parseInt(preferences.getString("gestureSwipeDown", "0"));
            int gestureSwipeRight = Integer.parseInt(preferences.getString("gestureSwipeRight", "17"));
            if (gestureSwipeUp != GESTURE_NOTHING ||
                    gestureSwipeDown != GESTURE_NOTHING ||
                    gestureSwipeRight != GESTURE_NOTHING) {
                mHasDrawerSwipeConflicts = true;
                super.disableDrawerSwipe();
            }
        } 
    }
}
