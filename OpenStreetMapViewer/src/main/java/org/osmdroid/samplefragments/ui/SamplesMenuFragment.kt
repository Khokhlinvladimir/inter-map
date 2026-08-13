package org.osmdroid.samplefragments.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.osmdroid.ExtraSamplesActivity
import org.osmdroid.ISampleFactory
import org.osmdroid.R
import org.osmdroid.model.IBaseActivity
import org.osmdroid.samplefragments.BaseSampleFragment
import java.util.Collections
import java.util.Locale

/**
 * http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
 *
 *
 * created on 1/1/2017.
 *
 * @author Alex O'Ree
 */
class SamplesMenuFragment : Fragment() {
    private var savedState: Bundle? = null
    private var sampleFactory: ISampleFactory? = null
    private var additionActivitybasedSamples: MutableList<IBaseActivity?>? = null

    var listAdapter: ExpandableListAdapter? = null
    var expListView: ExpandableListView? = null
    var listDataHeader: MutableList<String?>? = null
    var listDataChild: HashMap<String?, MutableList<String?>?>? = null
    var titleSampleMap: MutableMap<String?, Any?> = HashMap<String?, Any?>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_menu_layout, container, false)


        //http://stackoverflow.com/a/15314508/1203182


        /* (...) */

        /* If the Fragment was destroyed inbetween (screen rotation), we need to recover the savedState first */
        /* However, if it was not, it stays in the instance from the last onDestroyView() and we don't want to overwrite it */
        if (savedInstanceState != null && savedState == null) {
            savedState = savedInstanceState.getBundle(TAG)
        }
        if (savedState != null) {
            if (sampleFactory != null) {
                //do nothing
            } else {
                val factory = savedState!!.getString("factory")
                var acts: ArrayList<String?>? = null
                if (savedState!!.containsKey("acts")) acts = savedState!!.getStringArrayList("acts")
                try {
                    val aClass = Class.forName(factory)
                    val method = aClass.getMethod("getInstance")
                    sampleFactory = method.invoke(null) as ISampleFactory?
                    if (acts == null) {
                        additionActivitybasedSamples = ArrayList()
                    } else {
                        //restore the list
                        additionActivitybasedSamples = ArrayList<IBaseActivity?>()
                        for (i in acts.indices) {
                            additionActivitybasedSamples!!.add(Class.forName(acts.get(i)).newInstance() as IBaseActivity)
                        }
                    }
                } catch (t: Throwable) {
                    //can resume for some reason
                    t.printStackTrace()
                    getActivity()!!.finish()
                }
            }
        }
        savedState = null


        // get the listview
        expListView = root.findViewById<ExpandableListView?>(R.id.lvExp)


        // Listview on child click listener
        expListView!!.setOnChildClickListener(object : OnChildClickListener {
            override fun onChildClick(
                parent: ExpandableListView?, v: View?,
                groupPosition: Int, childPosition: Int, id: Long
            ): Boolean {
                val title = listDataChild!!.get(
                    listDataHeader!!.get(groupPosition)
                )!!.get(
                    childPosition
                )
                val o = titleSampleMap.get(title)
                if (o != null && o is BaseSampleFragment) {
                    // Replace Fragment with selected sample
                    val frag = o
                    Log.i(TAG, "loading fragment " + frag.sampleTitle + ", " + frag.javaClass.getCanonicalName())
                    (activity as? ExtraSamplesActivity)?.setSampleTitle(frag.sampleTitle)
                    val fm = getFragmentManager()
                    fm!!.beginTransaction().replace(R.id.samples_container, frag, ExtraSamplesActivity.SAMPLES_FRAGMENT_TAG)
                        .addToBackStack(null).commit()
                } else if (o != null && o is IBaseActivity && o is Activity) {
                    val activity = o as IBaseActivity
                    val i = Intent(getContext(), activity.javaClass)
                    Log.i(TAG, "loading activity " + activity.activityTitle + ", " + activity.javaClass.getCanonicalName())
                    getActivity()!!.startActivity(i)
                } else if (o == null) {
                    //NOOP
                } else {
                    Toast.makeText(getActivity(), "Example is of an unexpected type, please report this", Toast.LENGTH_LONG).show()
                }
                return false
            }
        })

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //the following block of code took me an entire weekend to track down the root cause.
        //if the code block is in onCreate, it will leak. onActivityCreated = no leak.
        //makes no sense, but that's Android for you.

        // preparing list data
        val success = prepareListData()
        if (!success) {
            val act: Activity? = getActivity()
            act!!.finish()
            return
        }

        listAdapter = ExpandableListAdapter(getActivity()!!, listDataHeader!!, listDataChild!!)

        // setting list adapter
        expListView!!.setAdapter(listAdapter)
    }

    /*
     * Preparing the list data
     */
    private fun prepareListData(): Boolean {
        val headers: MutableSet<String?> = HashSet<String?>()
        listDataHeader = ArrayList<String?>()

        //category, content
        listDataChild = HashMap<String?, MutableList<String?>?>()
        if (sampleFactory == null || additionActivitybasedSamples == null) {
            //getActivity().getSupportFragmentManager().popBackStack();
            return false
        }
        //had this throw an NPE once after device rotation and a back button press.
        for (a in 0 until sampleFactory!!.count()) {
            val f = sampleFactory!!.getSample(a)
            titleSampleMap.put(f!!.sampleTitle, f)
            val clz = f.javaClass.getCanonicalName()
            val bits = clz!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var group = bits[bits.size - 2]
            group = capitialize(group)

            headers.add(group)

            if (!listDataChild!!.containsKey(group)) {
                listDataChild!!.put(group, ArrayList<String?>())
            }
            listDataChild!!.get(group)!!.add(f.sampleTitle)
        }


        if (!additionActivitybasedSamples!!.isEmpty()) {
            listDataHeader!!.add("Activities")
            listDataChild!!.put("Activities", ArrayList<String?>())
            for (a in additionActivitybasedSamples!!.indices) {
                listDataChild!!.get("Activities")!!.add(additionActivitybasedSamples!!.get(a)!!.activityTitle)
                titleSampleMap.put(additionActivitybasedSamples!!.get(a)!!.activityTitle, additionActivitybasedSamples!!.get(a))
            }
        }


        listDataHeader!!.addAll(headers)

        return true
    }

    private fun capitialize(group: String): String {
        var group = group
        if (group.get(0) >= 'a' && group.get(0) <= 'z') {
            val first = group.substring(0, 1).uppercase(Locale.getDefault())
            group = first + group.substring(1)
        }
        return group
    }


    override fun onResume() {
        super.onResume()
        (activity as? ExtraSamplesActivity)?.setSampleTitle(null)

        //FragmentManager fm = getFragmentManager();
        //fm.popBackStack();
        //System.gc();
    }

    override fun onDestroyView() {
        expListView = null
        savedState = saveState()
        super.onDestroyView()
    }

    private fun saveState(): Bundle { /* called either from onDestroyView() or onSaveInstanceState() */
        val state = Bundle()
        val currentFactory = sampleFactory
        if (currentFactory != null)  //yup, hate android
            state.putString("factory", currentFactory.javaClass.getCanonicalName())
        if (additionActivitybasedSamples != null) {
            val actClasses = ArrayList<String?>()
            for (i in additionActivitybasedSamples!!.indices) actClasses.add(additionActivitybasedSamples!!.get(i)!!.javaClass.getCanonicalName())
            state.putStringArrayList("acts", actClasses)
        }
        return state
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        /* If onDestroyView() is called first, we can use the previously savedState but we can't call saveState() anymore */
        /* If onSaveInstanceState() is called first, we don't have savedState, so we need to call saveState() */
        /* => (?:) operator inevitable! */
        outState.putBundle(TAG, if (savedState != null) savedState else saveState())
    }

    companion object {
        const val TAG: String = "osmfragsample"

        fun newInstance(fac: ISampleFactory?, additionActivitybasedSamples: MutableList<IBaseActivity?>?): SamplesMenuFragment {
            val x = SamplesMenuFragment()
            x.sampleFactory = fac
            x.additionActivitybasedSamples = additionActivitybasedSamples
            return x
        }
    }
}
