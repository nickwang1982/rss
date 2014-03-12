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

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public
class ListFragmentTag extends Fragment
{
   private static final String POSITION_KEY = "POSITION";
   private ListView m_listView;
   static boolean s_firstLoad = true;
   static final int LIST_VIEW_ID_BASE = 20000;

   static
   Fragment newInstance(int position)
   {
      Fragment fragment = new ListFragmentTag();
      Bundle bundle = new Bundle();
      bundle.putInt(POSITION_KEY, position);
      fragment.setArguments(bundle);
      return fragment;
   }

   @Override
   public
   View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {
      final Activity activity = getActivity();

      RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.list_view_tag, null, false);
      m_listView = (ListView) layout.findViewById(R.id.list_view);

      m_listView.setId(LIST_VIEW_ID_BASE + getArguments().getInt(POSITION_KEY));
      m_listView.setOnScrollListener(new AbsListView.OnScrollListener()
      {
         private static final int TOUCH = AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
         private static final int IDLE = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

         @Override
         public
         void onScrollStateChanged(AbsListView view, int scrollState)
         {
            if(TOUCH == scrollState || IDLE == scrollState)
            {
               Adapter adapter = view.getAdapter();
               int first = view.getFirstVisiblePosition();
               int last = view.getLastVisiblePosition();

               for(int i = 0; last - first >= i; i++)
               {
                  View viewItem = view.getChildAt(i);

                  if(null != viewItem && viewItem.isShown() && 0 <= viewItem.getTop())
                  {
                     FeedItem item = (FeedItem) adapter.getItem(first + i);
                     AdapterTags.READ_ITEM_TIMES.add(item.m_time);
                  }
               }
            }
            if(IDLE == scrollState)
            {
               AsyncNavigationAdapter.update(activity);
            }
         }

         @Override
         public
         void onScroll(AbsListView v, int fir, int visible, int total)
         {
         }
      });
      registerForContextMenu(m_listView);

      return layout;
   }

   @Override
   public
   void onActivityCreated(Bundle savedInstanceState)
   {
      super.onActivityCreated(savedInstanceState);

      m_listView.setAdapter(new AdapterTags(getActivity()));
      m_listView.setEmptyView(((View) m_listView.getParent()).findViewById(R.id.empty));
      if(s_firstLoad)
      {
         AsyncNewTagAdapters.update(getActivity());
         s_firstLoad = false;
      }
   }

   @Override
   public
   void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
   {
      super.onCreateContextMenu(menu, v, menuInfo);

      /* Inflate the context menu from the xml file. */
      getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);

      /* Set the title of the context menu to the feed item's title. */
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      FeedItem view = (FeedItem) ((AdapterView<ListAdapter>) v).getAdapter().getItem(info.position);
      menu.setHeaderTitle(view.m_title);
   }

   @Override
   public
   boolean onContextItemSelected(MenuItem item)
   {
      /* Get the feed url from the FeedItem. */
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      FeedItem feedItem = ((ViewFeedItem) info.targetView).m_item;
      String url = feedItem.m_urlFull;

      Context context = getActivity();

      if(R.id.copy == item.getItemId())
      {
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setPrimaryClip(ClipData.newPlainText("Url", url));

         Toast toast = Toast.makeText(context, getString(R.string.toast_url_copied) + ' ' + url, Toast.LENGTH_SHORT);
         toast.show();
         return true;
      }
      if(R.id.open == item.getItemId())
      {
         context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
         return true;
      }
      return false;
   }
}
