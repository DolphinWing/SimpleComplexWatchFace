@file:Suppress("PackageName")

package dolphin.android.wear.SimpleComplexFace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.wear.widget.WearableRecyclerView
import android.support.wearable.activity.WearableActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class ColorPickerActivity : WearableActivity() {
    companion object {
        const val KEY_COLOR = "color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config2)
        findViewById<WearableRecyclerView>(R.id.recycler_config_view)?.let {
            it.adapter = MyAdapter(this)
            it.layoutManager = LinearLayoutManager(this)
            it.setHasFixedSize(true)
        }
    }

    private class MyAdapter(private val activity: Activity)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val configs = Configs(activity)
        private val colors = intArrayOf(
                configs.COLOR_WHITE, configs.COLOR_RED, configs.COLOR_GREEN, configs.COLOR_BLUE,
                configs.COLOR_ORANGE, configs.COLOR_PURPLE
        )

        override fun getItemCount() = colors.count()

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            return ColorHolder(activity, LayoutInflater.from(parent?.context)
                    .inflate(R.layout.holder_color, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            holder?.let {
                (it as ColorHolder).apply {
                    itemView.tag = colors[position]
                    icon.setImageResource(configs.getColorDrawable(colors[position]))
                    text.visibility = View.VISIBLE
                }
            }
        }
    }

    private class ColorHolder(private val activity: Activity, view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {
        var text: TextView = view.findViewById(android.R.id.message)
        var icon: ImageView = view.findViewById(android.R.id.icon)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            //start activity for result
            val data = Intent()
            data.putExtra(KEY_COLOR, Integer.parseInt(view?.tag.toString()))
            activity.setResult(Activity.RESULT_OK, data)
            activity.finish()
        }
    }
}
