/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.poloure.simplerss;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;

class Utilities
{
   static final int EIGHT_DP = getDp(8.0F);

   static
   void showMenuItems(Menu menu, boolean add, boolean unread, boolean refresh)
   {
      MenuItem addFeedMenu = menu.findItem(R.id.add_feed);
      MenuItem unreadMenu = menu.findItem(R.id.unread);
      MenuItem refreshMenu = menu.findItem(R.id.refresh);

      if(null != refreshMenu && null != unreadMenu && null != addFeedMenu)
      {
         addFeedMenu.setVisible(add);
         unreadMenu.setVisible(unread);
         refreshMenu.setVisible(refresh);
      }
   }

   static
   void updateSubtitleCount(Activity activity, int page)
   {
      ListView navigationList = (ListView) activity.findViewById(R.id.navigation_list);
      if(null != navigationList)
      {
         ActionBar actionBar = activity.getActionBar();
         if(null != actionBar)
         {
            if(-1 == page)
            {
               actionBar.setSubtitle(null);
            }
            else
            {
               Adapter adapter = navigationList.getAdapter();
               String unreadCount = (String) adapter.getItem(page);

               String unreadText = activity.getString(R.string.subtitle_unread);
               actionBar.setSubtitle(unreadText + ' ' + unreadCount);
            }
         }
      }
   }

   static
   int getDp(float pixels)
   {
      Resources resources = Resources.getSystem();
      DisplayMetrics metrics = resources.getDisplayMetrics();
      float floatDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels, metrics);
      return Math.round(floatDp);
   }

   static
   void setPaddingEqual(View view, int padding)
   {
      view.setPadding(padding, padding, padding, padding);
   }
}
