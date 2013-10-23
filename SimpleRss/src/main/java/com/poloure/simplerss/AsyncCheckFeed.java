package com.poloure.simplerss;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AsyncCheckFeed extends AsyncTask<Void, Void, String[]>
{
   /* Formats */
   static final         String  INDEX_FORMAT            = "feed|%s|url|%s|tag|%s|";
   static final         int     MODE_ADD_FEED           = 1;
   static final         int     MODE_EDIT_FEED          = 2;
   private static final String  TAG_TITLE               = "<title";
   private static final String  END_TAG_TITLE           = "</title>";
   private static final Pattern ILLEGAL_FILE_CHARS      = Pattern.compile("[/\\?%*|<>:]");
   private static final Pattern SPLIT_SPACE             = Pattern.compile(" ");
   private static final int     FEED_STREAM_BYTE_BUFFER = 512;
   private final AlertDialog m_dialog;
   private final int         m_mode;
   private final String      m_oldFeedName;
   private final Context     m_context;
   private       boolean     m_isFeedReal;

   AsyncCheckFeed(AlertDialog dialog, int mode, String currentTitle, Context context)
   {
      m_dialog = dialog;
      m_mode = mode;
      m_oldFeedName = currentTitle;
      m_context = context;
      Button button = m_dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      button.setEnabled(false);
   }

   @Override
   protected
   String[] doInBackground(Void... nothing)
   {
      String userInputTags = ((TextView) m_dialog.findViewById(R.id.tag_edit)).getText().toString();
      Locale defaultLocale = Locale.getDefault();

      String initialTags = 0 == userInputTags.length()
            ? m_context.getString(R.string.all_tag)
            : userInputTags.toLowerCase(defaultLocale);

      int tagInitialCapacity = initialTags.length();

      /* Capitalise each word. */
      String[] words = SPLIT_SPACE.split(initialTags);
      StringBuilder tagBuilder = new StringBuilder(tagInitialCapacity);

      for(String word : words)
      {
         String firstLetter = word.substring(0, 1);
         String restOfWord = word.substring(1);

         String firstLetterUpper = firstLetter.toUpperCase(defaultLocale);
         String restOfWordLower = restOfWord.toLowerCase(defaultLocale);

         tagBuilder.append(firstLetterUpper);
         tagBuilder.append(restOfWordLower);
         tagBuilder.append(' ');
      }

      int tagLength = tagBuilder.length();
      int lastChar = tagLength - 1;
      tagBuilder.delete(lastChar, tagLength);
      String finalTag = tagBuilder.toString();

      String userInputUrl = ((TextView) m_dialog.findViewById(R.id.feed_url_edit)).getText()
            .toString();

      String[] checkList = userInputUrl.contains("http")
            ? new String[]{userInputUrl}
            : new String[]{
                  "http://" + userInputUrl, "https://" + userInputUrl
            };

      String feedUrl = "";
      String feedTitle = "";

      for(String check : checkList)
      {
         try
         {
            BufferedInputStream in = null;
            try
            {
               URL url1 = new URL(check);
               InputStream inputStream = url1.openStream();
               in = new BufferedInputStream(inputStream);
               byte[] data = new byte[FEED_STREAM_BYTE_BUFFER];
               in.read(data, 0, FEED_STREAM_BYTE_BUFFER);

               String line = new String(data);
               if(line.contains("rss") || line.contains("Atom") || line.contains("atom"))
               {
                  while(!line.contains(TAG_TITLE) && !line.contains(END_TAG_TITLE))
                  {
                     byte[] data2 = new byte[FEED_STREAM_BYTE_BUFFER];
                     in.read(data2, 0, FEED_STREAM_BYTE_BUFFER);
                     data = joinByteArrays(data, data2);
                     line = new String(data);
                  }

                  int tagTitleIndex = line.indexOf(TAG_TITLE);
                  int moreIndex = line.indexOf('>', tagTitleIndex + 1);
                  int lessIndex = line.indexOf("</", moreIndex);

                  feedTitle = line.substring(moreIndex, lessIndex);
                  m_isFeedReal = true;
                  feedUrl = check;
               }
            }
            finally
            {
               if(null != in)
               {
                  in.close();
               }
            }
         }
         catch(MalformedURLException e)
         {
            e.printStackTrace();
         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
      }
      return new String[]{feedUrl, feedTitle, finalTag};
   }

   @Override
   protected
   void onPostExecute(String[] result)
   {
      if(!m_isFeedReal)
      {
         Button button = m_dialog.getButton(DialogInterface.BUTTON_POSITIVE);
         if(null != button)
         {
            button.setEnabled(true);
         }
         return;
      }

      String feedUrlFromCheck = result[0];
      String feedTitleFromXml = result[1];
      String feedTag = result[2];

      String userInputName = ((TextView) m_dialog.findViewById(R.id.name_edit)).getText()
            .toString();

      /* Did the user enter a feed name? If not, use the feed title found from the check. */
      String finalName = 0 == userInputName.length() ? feedTitleFromXml : userInputName;

      /* Replace any characters that are not allowed in file names. */
      Matcher matcher = ILLEGAL_FILE_CHARS.matcher(finalName);
      finalName = matcher.replaceAll("");

      /* Create the csv. */
      String feedInfo = String.format(INDEX_FORMAT, finalName, feedUrlFromCheck, feedTag) +
            System.getProperty("line.separator");

      if(MODE_EDIT_FEED == m_mode)
      {
         editFeed(m_oldFeedName, finalName);
      }

      /* Save the feed to the index. */
      Write.single(Read.INDEX, feedInfo, m_context);

      /* Update the tags. */
      updateTags((Activity) m_context);

      /* Update the manage ListView adapters. */
      FragmentManager fragmentManager = ((FragmentActivity) m_context).getSupportFragmentManager();

      String tagPrefix = "android:switcher:" + FragmentManage.VIEW_PAGER_ID + ':';
      String tagTag = tagPrefix + 0;
      String feedsTag = tagPrefix + 1;

      ListFragment tagFragment = (ListFragment) fragmentManager.findFragmentByTag(tagTag);
      ListFragment feedsFragment = (ListFragment) fragmentManager.findFragmentByTag(feedsTag);

      ListView tagListView = tagFragment.getListView();
      ListView feedsListView = feedsFragment.getListView();

      FragmentManageTags.asyncCompatManageTagsRefresh(tagListView, m_context);
      FragmentManageFeeds.asyncCompatManageFeedsRefresh(feedsListView, m_context);

      m_dialog.dismiss();
   }

   static
   void updateTags(Activity activity)
   {
      ViewPager tagPager = (ViewPager) activity.findViewById(FragmentFeeds.VIEW_PAGER_ID);

      PagerAdapter pagerAdapter = tagPager.getAdapter();
      ((PagerAdapterFeeds) pagerAdapter).getTagsFromDisk(activity);
      pagerAdapter.notifyDataSetChanged();

      Update.navigation(activity);
   }

   private
   void editFeed(CharSequence oldFeed, String newFeed)
   {
      /* Rename the folder if it is different. */
      String oldFeedFolder = oldFeed + File.separator;
      String newFeedFolder = newFeed + File.separatorChar;

      if(!oldFeed.equals(newFeed))
      {
         Util.moveFile(oldFeedFolder, newFeedFolder, m_context);
      }

      /* Replace the all_tag file with the new image and data. */
      Write.removeLine(Read.INDEX, oldFeed, true, m_context);

   }

   private static
   byte[] joinByteArrays(byte[] first, byte... second)
   {
      if(null == first)
      {
         return second;
      }
      if(null == second)
      {
         return first;
      }
      byte[] result = new byte[first.length + second.length];
      System.arraycopy(first, 0, result, 0, first.length);
      System.arraycopy(second, 0, result, first.length, second.length);
      return result;
   }
}
