package yay.poloure.simplerss;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class read
{
   public static String setting(String path)
   {
      String[] check = read.file(path);
      return (check.length == 0) ? "" : check[0];
   }

   public static String[][] csv(String file_path, char... type)
   {
      int next, offset, i, j;
      String line;
      String[][] types;
      String[] lines;
      char ch;

      lines = file(file_path);
      if(lines.length == 0)
         return new String[0][0];

      types = new String[type.length][lines.length];

      for(j = 0; j < lines.length; j++)
      {
         offset = 0;
         line = lines[j];
         while((next = line.indexOf('|', offset)) != -1)
         {
            if(offset == line.length())
               break;

            ch = line.charAt(offset);
            offset = next + 1;
            for(i = 0; i < type.length; i++)
            {
               if(ch == type[i])
               {
                  next = line.indexOf('|', offset);
                  types[i][j] = line.substring(offset, next);
                  break;
               }
            }
            offset = line.indexOf('|', offset) + 1;
         }
      }
      return types;
   }

   /* This function is now safe. It will return a zero length array on error. */
   public static String[] file(String file_path)
   {
      final String count_path = file_path + main.COUNT;
      final int count;
      String line;

      /* If the file_path is not a count file, get the number of lines. */
      if(!file_path.contains(main.COUNT))
      {
         String[] temp = file(count_path);
         count = (temp.length == 0) ? count(file_path) : util.stoi(temp[0]);
      }
      else
         count = count(count_path);

      /* If the file is empty, return a zero length array. */
      if(count == 0)
         return new String[0];

      /* Use the count to allocate memory for the array. */
      String[] lines = new String[count];

      /* Begin reading the file to the String array. */
      BufferedReader in = null;
      try
      {
         try
         {
            in = new BufferedReader(new FileReader(file_path));
            for(int i = 0; i < lines.length; i++)
               lines[i] = in.readLine();
            in.close();
         }
         finally
         {
            if(in != null)
               in.close();
         }
      }
      catch(IOException e)
      {
         return new String[0];
      }
      return lines;
   }

   public static Set<String> set(String file_path)
   {
      Set set = new HashSet<String>();
      java.util.Collections.addAll(set, file(file_path));
      return set;
   }

   public static int count(String file_path)
   {
      BufferedReader in = null;
      int i = 0;
      try
      {
         try
         {
            in = new BufferedReader(new FileReader(file_path));
            while(in.readLine() != null)
               i++;
         }
         finally
         {
            if(in != null)
               in.close();
         }
      }
      catch(IOException e)
      {
      }
      return i;
   }
}