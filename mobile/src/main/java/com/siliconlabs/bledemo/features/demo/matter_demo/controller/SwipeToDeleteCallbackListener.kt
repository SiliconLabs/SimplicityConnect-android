import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.demo.matter_demo.adapters.MatterScannedResultAdapter

class SwipeToDeleteCallback(adapter: MatterScannedResultAdapter) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private val adapter // Replace with your RecyclerView adapter
            : MatterScannedResultAdapter

    init {
        this.adapter = adapter
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
      //  adapter.deleteItem(position)
    }
}