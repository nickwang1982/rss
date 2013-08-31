package yay.poloure.simplerss;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import android.text.format.Time;

public class write
{
   public static void collection(String path, Iterable<?> content)
   {
      util.rm(path);
      try
      {
         BufferedWriter out = new BufferedWriter(new FileWriter(path, true));
         for(Object item : content)
         {
            out.write(item.toString() + main.NL);
         }
         out.close();
      }
      catch(Exception e)
      {
      }
   }

   /* Function should be safe, returns false if fails. */
   public static boolean dl(String urler, String file_path)
   {
      /* Check to see if we can write to the media. */
      if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
         return false;

      try
      {
         BufferedInputStream in = null;
         FileOutputStream fout = null;
         try
         {
            in = new BufferedInputStream(new URL(urler).openStream());
            fout = new FileOutputStream(file_path);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
               fout.write(data, 0, count);
         }
         finally
         {
            if (in != null)
               in.close();
            if (fout != null)
               fout.close();
         }
      }
      catch(Exception e)
      {
         util.rm(file_path);
         return false;
      }
      File file = new File(file_path);
      return (file.exists() && file.length() != 0);
   }

   /* Function should be safe, returns false if fails. */
   public static boolean single(String file_path, String string)
   {
      /* Check to see if we can write to the media. */
      if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
         return false;

      BufferedWriter out = null;
      try
      {
         try
         {
            out = new BufferedWriter(new FileWriter(file_path, true));
            out.write(string);
         }
         finally
         {
            if(out != null)
               out.close();
         }
      }
      catch (Exception e)
      {
         util.rm(file_path);
         return false;
      }
      return true;
   }

   /* This function should be safe, returns false if it failed. */
   public static boolean remove_string(String file_path, String string, Boolean contains)
   {
      /* Check to see if we can write to the media. */
      if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
         return false;

      final String temp_path  = file_path + main.TEMP;
      final String[] lines;
      BufferedWriter out      = null;
      try
      {
         try
         {
            /* Read the file to an array, if the file does not exist, return. */
            lines = read.file(file_path);
            if(lines.length == 0)
               return false;

            out = new BufferedWriter(new FileWriter(temp_path, true));

            for(String item : lines)
            {
               if(contains && !item.contains(string))
                  out.write(item + main.NL);
               else if(!contains && !item.equals(string))
                  out.write(item + main.NL);
            }
         }
         finally
         {
            if(out != null)
               out.close();
         }
      }
      /* If writing to the temp file fails, delete the temp file and return. */
      catch(Exception e)
      {
         util.rm(temp_path);
         return false;
      }

      /* If the rename failed, delete the file and write the original. */
      boolean success = util.mv(temp_path, file_path);
      if(!success)
      {
         util.rm(file_path);
         collection(file_path, java.util.Arrays.asList(lines));
      }
      return success;
   }

   public static void sort_content(String storage, String group, String all_group)
   {
      String sep                = main.SEPAR;
      String group_dir          = storage + main.GROUPS_DIR + group + sep;
      String group_content_path = group_dir + group + main.CONTENT;
      String group_count_file   = group_content_path + main.COUNT;
      String url_path           = group_content_path + main.URL;
      String url_count          = url_path + main.COUNT;

      String[][] contents = read.csv(group_dir + group + main.TXT, 'n', 'g');
      String[] names      = contents[0];
      String[] groups     = contents[1];
      String[] urls       = new String[0];
      String[][] temp;

      String content_path;
      Time time = new Time();
      String[] pubDates;
      String[] content;
      Map<Long, String> map = new TreeMap<Long, String>();

      for(int k = 0; k < names.length; k++)
      {
         /* "/storage/groups/Tumblr/mariam/mariam.content.txt" */
         content_path = storage + main.GROUPS_DIR + groups[k] + sep + names[k]
                        + sep + names[k] + main.CONTENT;

         if(util.exists(content_path))
         {
            temp     = read.csv(content_path, 'p', 'l');
            content  = read.file(content_path);
            pubDates = temp[0];
            urls     = util.concat(urls, temp[1]);

            for(int i = 0; i < pubDates.length; i++)
            {
               try
               {
                  time.parse3339(pubDates[i]);
               }
               catch(Exception e)
               {
                  break;
               }
               map.put(time.toMillis(false) - i, content[i] + "group|"
                       + groups[k] + "|feed|" + names[k] + "|");
            }
         }
      }

      try
      {
         BufferedWriter out = null;

         out = new BufferedWriter(new FileWriter(group_content_path, false));
         for(Map.Entry<Long, String> entry : map.entrySet())
         {
            out.write(entry.getValue().concat(main.NL));
         }
         out.close();

         util.rm(group_count_file);
         single(group_count_file, Integer.toString(map.size()));

         out = new BufferedWriter(new FileWriter(url_path, false));
         for(String url : urls)
         {
            out.write(url.concat(main.NL));
         }
         out.close();

         util.rm(url_count);
         single(url_count, Integer.toString(urls.length));
      }
      catch(Exception e)
      {
         log(storage, "Failed to write the group content file.");
      }

      /* Sorts the all_group every time another group is updated. */
      if(!group.equals(all_group))
      {
         sort_content(storage, all_group, all_group);
      }
   }

   public static void log(String storage, String text)
   {
      single(storage + main.DUMP_FILE, text + main.NL);
   }
}