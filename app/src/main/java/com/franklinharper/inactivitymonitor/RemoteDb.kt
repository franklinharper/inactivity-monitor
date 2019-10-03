package com.franklinharper.inactivitymonitor

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import java.time.temporal.IsoFields

data class EventsKey private constructor(
  val year: Int,
  val weekOfYear: Int
) {

  companion object {

    fun from(timestamp: Timestamp): EventsKey {
      val zonedDateTime = timestamp.toZonedDateTime()

      val year = zonedDateTime.get(IsoFields.WEEK_BASED_YEAR)
      if (year < 2019) {
        throw IllegalArgumentException("The year must be >= 2019. year:$year")
      }
      val weekOfYear = zonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
      return EventsKey(year, weekOfYear)
    }

  }

}

// Firestore is a schemaless document oriented DB.
//
// To reduce the risks of writing incorrect data, each method of this class
// validates the data written to and read from the DB.
//
class RemoteDb(
  val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

  fun writeEvents(key: EventsKey, events: Collection<Event>, onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit) {
    Timber.d("key:$key events: $events")
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
      throw IllegalStateException("The Firebase user is null")
    }

    val data = hashMapOf("events" to events)
    db.collection("users")
      .document(user.uid)
      .collection("eventsByWeek")
      .document("${key.year}-${key.weekOfYear}")
      .set(data, SetOptions.merge())
      .addOnSuccessListener { onSuccess() }
      .addOnFailureListener { exception -> onFailure(exception) }
  }

}