package yay.poloure.simplerss;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

class fragment_manage_filters extends Fragment
{
   static ListView filter_list;
   static adapter_manage_filter filter_list_adapter;

   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {
      final View view = inflater.inflate(R.layout.manage_listviews, container, false);
      filter_list = (ListView) view.findViewById(R.id.manage_listview);
      filter_list_adapter = new adapter_manage_filter(getActivity());
      filter_list.setAdapter(filter_list_adapter);

      final String filter_path = util.get_storage() + main.FILTER_LIST;

      filter_list_adapter.set_items(read.file(filter_path));

      filter_list.setOnItemLongClickListener
      (
         new OnItemLongClickListener()
         {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
            {
               AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
               builder.setCancelable(true)
               .setPositiveButton(main.DELETE_DIALOG, new DialogInterface.OnClickListener()
               {
                  @Override
                  public void onClick(DialogInterface dialog, int id)
                  {
                     write.remove_string(filter_path, filter_list_adapter.getItem(position), false);
                     filter_list_adapter.remove_item(position);
                     filter_list_adapter.notifyDataSetChanged();
                  }
               });
               AlertDialog alert = builder.create();
               alert.show();
               return true;
            }
         }
      );
      return view;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      if(navigation_drawer.drawer_toggle.onOptionsItemSelected(item))
         return true;
      else if(item.getTitle().equals("add"))
      {
         add_edit_dialog.show_add_filter_dialog(getActivity());
         return true;
      }
      return super.onOptionsItemSelected(item);
   }
}