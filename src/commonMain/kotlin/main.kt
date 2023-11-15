import wayland.wl_display_connect
import wayland.wl_display_disconnect
import wayland.wl_display_flush
import wayland.wl_display_get_registry
import wayland.wl_registry_destroy

fun main() {
	println("Hello!")
	val display = checkNotNull(wl_display_connect(null)) {
		"Failed to connect to the Wayland display."
	}
	println("Got display!")
	try {
		val registry = checkNotNull(wl_display_get_registry(display)) {
			"Failed to get the wayland registry."
		}
		println("Got registry!")
		wl_registry_destroy(registry)
		println("Destroyed registry")
	} finally {
		wl_display_flush(display)
		wl_display_disconnect(display)
		println("Disconnected display")
	}
}
