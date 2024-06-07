
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.l3s.dronegpt.data.database.Experiment
import dji.sampleV5.aircraft.R

class ExperimentAdapter(private val onClick: (Experiment) -> Unit) : ListAdapter<Experiment, ExperimentAdapter.ExperimentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExperimentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dronegpt_experiment_item, parent, false)
        return ExperimentViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ExperimentViewHolder, position: Int) {
        val experiment = getItem(position)
        holder.bind(experiment)
    }

    class ExperimentViewHolder(private val view: View, val onClick: (Experiment) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(experiment: Experiment) {
            val experimentName = "Experiment ${experiment.id.toString()}"
            view.findViewById<TextView>(R.id.experiment_name).text = experimentName
            view.setOnClickListener {
                onClick(experiment)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Experiment>() {
        override fun areItemsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
            return oldItem == newItem
        }
    }
}
