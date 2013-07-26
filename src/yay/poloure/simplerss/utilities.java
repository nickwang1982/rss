package yay.poloure.simplerss;

import android.content.Context;
import android.content.DialogInterface;
import android.app.FragmentManager;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.view.ViewPager;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;

import android.view.LayoutInflater;
import android.view.View;

import android.os.Debug;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.SpinnerAdapter;
import android.widget.AdapterView;

public class utilities
{
	public static void add_feed(String storage, String feed_name, String feed_url, String feed_group, String all_string)
	{
		append_string_to_file(storage + "groups/" + feed_group + ".txt", "name|" +  feed_name + "|url|" + feed_url + "|\n");
		append_string_to_file(storage + "groups/" + all_string + ".txt", "name|" +  feed_name + "|url|" + feed_url + "|group|" + feed_group + "|\n");

		if(main_view.feed_list_adapter != null)
			new main_view.refresh_manage_feeds().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		if(main_view.group_list_adapter != null)
			new main_view.refresh_manage_groups().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void edit_feed(String storage, String old_name, String new_name, String new_url, String old_group, String new_group, int position, String all_string)
	{
		remove_string_from_file(storage + "groups/" + all_string + ".txt", old_name, true);
		append_string_to_file(storage + "groups/" + all_string + ".txt", "name|" +  new_name + "|url|" + new_url + "|group|" + new_group + "|\n");

		if(!old_name.equals(new_name))
			(new File(storage + "content/" + old_name + ".store.txt.content.txt"))
			.renameTo((new File(storage + "content/" + new_name + ".store.txt.content.txt")));

		if(!old_group.equals(new_group))
		{
			remove_string_from_file(storage + "groups/" + old_group + ".txt", old_name, true);
			append_string_to_file(storage + "groups/" + new_group + ".txt", "name|" +  new_name + "|url|" + new_url + "|\n");

			delete_if_empty("groups/" + old_group + ".txt");
			if(!exists("groups/" + old_group + ".txt"))
				remove_string_from_file(storage + main_view.GROUP_LIST, old_group, false);
		}
		else
		{
			remove_string_from_file(storage + "groups/" + old_group + ".txt", old_name, true);
			append_string_to_file(storage + "groups/" + old_group + ".txt", "name|" +  new_name + "|url|" + new_url + "|\n");
		}

		main_view.feed_list_adapter.set_position(position, new_name, new_url + "\n" + new_group + " • " + Integer.toString(count_lines(storage + "content/" + new_name + ".store.txt.content.txt") - 1) + " items");
		main_view.feed_list_adapter.notifyDataSetChanged();

		main_view.update_groups("");
		new main_view.refresh_manage_feeds().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		new main_view.refresh_manage_groups().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		sort_group_content_by_time(storage, all_string);
		if(exists("groups/" + old_group + ".txt"))
			sort_group_content_by_time(storage, old_group);
		if(exists("groups/" + new_group + ".txt"))
			sort_group_content_by_time(storage, new_group);
	}

	public static void add_group(String storage, String group_name)
	{
		append_string_to_file(storage + main_view.GROUP_LIST, group_name + "\n");
		main_view.update_groups("");
	}

	public static void delete_group(String storage, String group)
	{
		/// Move all feeds to an unsorted group.
		//delete(storage + "groups/" + group + ".txt");
		//delete(storage + "groups/" + group + ".txt.content.txt");
	}

	public static void save_positions(FragmentManager fragment_manager, ViewPager viewpager, String storage)
	{
		adapter_feeds_cards adapter;
		BufferedWriter out;
		String url, group;
		String[] feeds;
		List<String> lines;
		Boolean found_url = false;
		List<String> current_groups = read_file_to_list(storage + main_view.GROUP_LIST);
		final int size = current_groups.size();

		for(int i = 1; i < size; i++)
		{
			try
			{
				group = current_groups.get(i);
				adapter = get_adapter_feeds_cards(fragment_manager, viewpager, i);
				if(adapter.getCount() > 0)
				{
					/// Read each of the content files from the group and find the line with the url.
					feeds = read_single_to_array(storage + "groups/" + group + ".txt", "name|");
					found_url = false;
					url = adapter.return_latest_url();
					if(!url.isEmpty())
					{
						for(String feed: feeds)
						{
							lines = read_file_to_list(storage + "content/" + feed + ".store.txt.content.txt");
							delete(storage + "content/" + feed + ".store.txt.content.txt");

							out = new BufferedWriter(new FileWriter(storage + "content/" + feed + ".store.txt.content.txt", true));
							for(String line : lines)
							{
								if(!found_url)
								{
									if(!line.contains(url))
										out.write(line + "\n");
									else if(!line.substring(0, 9).equals("marker|1|"))
									{
										out.write("marker|1|" + line + "\n");
										found_url = true;
									}
									else
										out.write(line + "\n");
								}
								else
									out.write(line + "\n");
							}
							out.close();
							if(found_url)
								break;
						}
						sort_group_content_by_time(storage, group);
					}
				}
			}
			catch(Exception e){
			}
		}
		if(found_url)
			sort_group_content_by_time(storage, current_groups.get(0));
	}

	public static void toast_message(Context activity_context, CharSequence message, final Boolean short_long)
	{
		Toast message_toast;
		if(short_long)
			message_toast = Toast.makeText(activity_context, message, Toast.LENGTH_SHORT);
		else
			message_toast = Toast.makeText(activity_context, message, Toast.LENGTH_LONG);
		message_toast.show();
	}

	public static byte[] concat_byte_arrays(byte[] a, byte... b)
	{
		final int a_length = a.length;
		final int b_length = b.length;
		byte[] c = new byte[a_length + b_length];
		System.arraycopy(a, 0, c, 0, a_length);
		System.arraycopy(b, 0, c, a_length, b_length);
		return c;
	}

	public static String[] remove_element(String[] a, int index)
	{
		String[] b = new String[a.length - 1];
		System.arraycopy(a, 0, b, 0, index);
		if(a.length != index)
			System.arraycopy(a, index + 1, b, index, a.length - index - 1);
		return b;
	}

	public static void download_file(String urler, String file_path)
	{
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
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
			catch(Exception e){
			}
		}
	}

	public static void append_string_to_file(String file_path, String string)
	{
		try
		{
			final BufferedWriter out = new BufferedWriter(new FileWriter(file_path, true));
			out.write(string);
			out.close();
		}
		catch (Exception e)
		{
		}
	}

	public static void remove_string_from_file(String file_path, String string, Boolean contains)
	{
		final List<String> list = read_file_to_list(file_path);
		delete(file_path);
		try{
			final BufferedWriter out = new BufferedWriter(new FileWriter(file_path, true));
			for(String item : list)
			{
				if(contains)
				{
					if(!item.contains(string))
						out.write(item + "\n");
				}
				else
				{
					if(!item.equals(string))
						out.write(item + "\n");
				}
			}
			out.close();
		}
		catch(Exception e){
		}
	}

	public static String[] read_single_to_array(String file_path, String type)
	{
		int next, offset, j;
		String line;
		char ch;
		List<String> lines = new ArrayList<String>();

		try
		{
			BufferedReader stream = new BufferedReader(new FileReader(file_path));
			while((line = stream.readLine()) != null)
				lines.add(line);
			stream.close();
		}
		catch(Exception e)
		{
		}

		String[] types = new String[lines.size()];

		for(j = 0; j < lines.size(); j++)
		{
			line = lines.get(j);
			if((next = line.indexOf(type, 0)) != -1)
			{
				ch = type.charAt(0);
				offset = next + 1;
				switch(ch)
				{
					case 'm':
						types[j] = "1";
						break;
					default:
						next = line.indexOf('|', offset);
						offset = next + 1;
						types[j] = line.substring(offset, line.indexOf('|', offset));
						break;
				}
			}
		}
		return types;
	}

	public static String[][] read_csv_to_array(String file_path, char... type)
	{
		int next, offset, k, j;
		String line;
		char ch;
		int start = (type[0] == 'm') ? 1 : 0;
		List<String> lines = new ArrayList<String>();

		try
		{
			BufferedReader stream = new BufferedReader(new FileReader(file_path));
			while((line = stream.readLine()) != null)
				lines.add(line);
			stream.close();
		}
		catch(Exception e)
		{
		}
		String[][] types = new String[type.length][lines.size()];

		for(j = 0; j < lines.size(); j++)
		{
			offset = 0;
			line = lines.get(j);
			while((next = line.indexOf('|', offset)) != -1)
			{
				//one = line.substring(offset, next);
				if(offset == line.length())
					break;

				ch = line.charAt(offset);
				offset = next + 1;
				switch(ch)
				{
					case 'm':
						types[0][j] = "1";
						break;
					default:
						for(k = start; k < type.length; k++)
						{
							if(ch == type[k])
							{
								next = line.indexOf('|', offset);
								types[k][j] = line.substring(offset, next);
								break;
							}
						}
						break;
				}
				offset = line.indexOf('|', offset) + 1;
			}
		}
		return types;
	}

	public static String[][] load_csv_to_array(String file_path)
	{
		int next, offset, j;
		String line;
		char ch;

		List<String> lines = new ArrayList<String>();
		try
		{
			BufferedReader stream = new BufferedReader(new FileReader(file_path));
			while((line = stream.readLine()) != null)
				lines.add(line);
			stream.close();
		}
		catch(Exception e)
		{
		}

		String[][] types = new String[7][lines.size()];

		for(j = 0; j < lines.size(); j++)
		{
			offset = 0;
			line = lines.get(j);
			while((next = line.indexOf('|', offset)) != -1)
			{
				if(offset == line.length())
					break;

				ch = line.charAt(offset);
				offset = next + 1;
				switch(ch)
				{
					case 'm':
						types[0][j] = "1";
						break;
					case 't':
						next = line.indexOf('|', offset);
						types[1][j]		= line.substring(offset, next);
						break;
					case 'd':
						next = line.indexOf('|', offset);
						types[2][j]		= line.substring(offset, next);
						break;
					case 'l':
						next = line.indexOf('|', offset);
						types[3][j]		= line.substring(offset, next);
						break;
					case 'i':
						next = line.indexOf('|', offset);
						types[4][j]		= line.substring(offset, next);
						break;
					case 'w':
						next = line.indexOf('|', offset);
						types[5][j]		= line.substring(offset, next);
						break;
					case 'h':
						next = line.indexOf('|', offset);
						types[6][j]		= line.substring(offset, next);
						break;
				}
				offset = line.indexOf('|', offset) + 1;
			}
		}
		return types;
	}

	public static List<String> read_file_to_list(String file_path)
	{
		String line;
		BufferedReader stream;
		List<String> lines = new ArrayList<String>();
		try
		{
			stream = new BufferedReader(new FileReader(file_path));
			while((line = stream.readLine()) != null)
				lines.add(line);
			stream.close();
		}
		catch(IOException e){
		}
		return lines;
	}

	public static int count_lines(String file_path)
	{
		BufferedReader stream;
		int i = 0;
		try
		{
			stream = new BufferedReader(new FileReader(file_path));
			while(stream.readLine() != null)
				i++;
			stream.close();
		}
		catch(IOException e){
		}
		return i;
	}

	public static adapter_feeds_cards get_adapter_feeds_cards(FragmentManager fragment_manager, ViewPager viewpager, int page_index)
	{
		return ((adapter_feeds_cards)((main_view.fragment_card) fragment_manager
						.findFragmentByTag("android:switcher:" + viewpager.getId() + ":" + Integer.toString(page_index)))
						.getListAdapter());
	}

	public static void sort_group_content_by_time(String storage, String group)
	{
		final String group_path = storage + "groups/" + group + ".txt.content.txt";
		String content_path;
		Time time = new Time();
		String[] pubDates;
		List<String> content;
		Map<Long, String> map = new TreeMap<Long, String>();
		int i;

		final String[] feeds_array = read_single_to_array(storage + "groups/" + group + ".txt", "name|");

		for(String feed : feeds_array)
		{
			content_path = storage + "content/" + feed + ".store.txt.content.txt";
			if(exists(content_path))
			{
				content 		= read_file_to_list(content_path);
				pubDates		= read_single_to_array(content_path, "pubDate|");

				if((pubDates[0] == null)||(pubDates[0].length() < 8))
					pubDates 	= read_single_to_array(content_path, "published|");

				for(i = 0; i < pubDates.length; i++)
				{
					try
					{
						time.parse3339(pubDates[i]);
					}
					catch(Exception e)
					{
						break;
					}
					map.put(time.toMillis(false) - i, content.get(i));
				}
			}
		}

		delete(group_path);
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(group_path, true));
			for(Map.Entry<Long, String> entry : map.entrySet())
			{
				out.write(entry.getValue() + "\n");
			}
			out.close();

			BufferedWriter out2 = new BufferedWriter(new FileWriter(group_path.concat(".count.txt"), false));
			out2.write(Integer.toString(map.size()));
			out2.close();
		}
		catch(Exception e)
		{
			log(storage, "Failed to write the group content file.");
		}
	}

	public static void show_add_dialog(final List<String> current_groups, Context activity_context)
	{
		final LayoutInflater inflater		= LayoutInflater.from(activity_context);
		final View add_rss_dialog			= inflater.inflate(R.layout.add_rss_dialog, null);
		final List<String> spinner_groups	= current_groups.subList(1, current_groups.size());
		final TextView group_edit			= (TextView) add_rss_dialog.findViewById(R.id.group_edit);
		final TextView URL_edit				= (TextView) add_rss_dialog.findViewById(R.id.URL_edit);
		final TextView name_edit			= (TextView) add_rss_dialog.findViewById(R.id.name_edit);
		final AdapterView<SpinnerAdapter> group_spinner	= (AdapterView<SpinnerAdapter>) add_rss_dialog.findViewById(R.id.group_spinner);

		final ArrayAdapter<String> adapter	= new ArrayAdapter<String>(activity_context, R.layout.group_spinner_text, spinner_groups);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		group_spinner.setAdapter(adapter);

		final AlertDialog alertDialog = new AlertDialog.Builder(activity_context, 2)
				.setTitle("Add Feed")
				.setView(add_rss_dialog)
				.setCancelable(true)
				.setNegativeButton
				(activity_context.getString(R.string.cancel_dialog), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog,int id)
						{
						}
					}
				)
				.create();

				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, (activity_context.getString(R.string.add_dialog)),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						final String new_group		= group_edit	.getText().toString().trim().toLowerCase();
						final String URL_check		= URL_edit		.getText().toString().trim();
						final String feed_name		= name_edit		.getText().toString().trim();
						final String spinner_group	= group_spinner	.getSelectedItem().toString();
						new check_feed_exists(alertDialog, new_group, feed_name, "add", spinner_group, "", "", 0, current_groups.get(0)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_check);
					}
				});
				alertDialog.show();
	}

	public static void show_edit_dialog(final List<String> current_groups, Context activity_context, String storage, final int position)
	{
		final LayoutInflater inflater		= LayoutInflater.from(activity_context);
		final View edit_rss_dialog			= inflater.inflate(R.layout.add_rss_dialog, null);
		final String[][] content			= utilities.read_csv_to_array(storage + "groups/"+ current_groups.get(0) + ".txt", 'n', 'u', 'g');
		final String current_title			= content[0][position];
		final String current_url			= content[1][position];
		final String current_group			= content[2][position];

		final TextView group_edit			= (TextView) edit_rss_dialog.findViewById(R.id.group_edit);
		final TextView URL_edit				= (TextView) edit_rss_dialog.findViewById(R.id.URL_edit);
		final TextView name_edit			= (TextView) edit_rss_dialog.findViewById(R.id.name_edit);
		final AdapterView<SpinnerAdapter> group_spinner	= (AdapterView<SpinnerAdapter>) edit_rss_dialog.findViewById(R.id.group_spinner);

		final List<String> spinner_groups	= current_groups.subList(1, current_groups.size());

		final ArrayAdapter<String> adapter	= new ArrayAdapter<String>(activity_context, R.layout.group_spinner_text, spinner_groups);
		adapter			.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		group_spinner	.setAdapter(adapter);
		URL_edit		.setText(current_url);
		name_edit		.setText(current_title);
		group_spinner	.setSelection(spinner_groups.indexOf(current_group));

		final AlertDialog edit_dialog = new AlertDialog.Builder(activity_context, 2)
				.setTitle(activity_context.getString(R.string.edit_dialog_title))
				.setView(edit_rss_dialog)
				.setCancelable(true)
				.setNegativeButton
				(activity_context.getString(R.string.cancel_dialog),new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog,int id)
						{
						}
					}
				)
				.create();

				edit_dialog.setButton(AlertDialog.BUTTON_POSITIVE, (activity_context.getString(R.string.accept_dialog)),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
							String new_group 		= group_edit	.getText().toString().trim().toLowerCase();
							String URL_check 		= URL_edit		.getText().toString().trim();
							String feed_name 		= name_edit		.getText().toString().trim();
							String spinner_group 	= group_spinner	.getSelectedItem().toString();
							new check_feed_exists(edit_dialog, new_group, feed_name, "edit", spinner_group, current_group, current_title, position, current_groups.get(0)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_check);
					}
				});

				edit_dialog.show();
	}

	public static void log(String storage, String text)
	{
		append_string_to_file(storage + "dump.txt", text + "\n");
	}

	public static void delete(String file_path)
	{
		(new File(file_path)).delete();
	}

	public static Boolean exists(String file_path)
	{
		return (new File(file_path)).exists();
	}

	public static void delete_if_empty(String file_path)
	{
		File file = new File(file_path);
		if(file.exists() && file.length() == 0)
			file.delete();
	}
}
