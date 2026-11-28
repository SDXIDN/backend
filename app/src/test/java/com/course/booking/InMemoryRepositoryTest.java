package com.course.booking;

import com.course.booking.venue.Venue;
import com.course.booking.venue.VenueRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRepositoryTest {
    private final VenueRepository repository = new VenueRepository();

    @Test
    void save_newEntities_getSequentialIds() {
        Venue first = repository.save(new Venue("A", "addr", "Kyiv"));
        Venue second = repository.save(new Venue("B", "addr", "Lviv"));

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(2L);
    }

    @Test
    void save_existingId_overwritesInsteadOfAdding() {
        Venue venue = repository.save(new Venue("A", "addr", "Kyiv"));
        Venue replacement = new Venue("A2", "addr2", "Kyiv");
        replacement.setId(venue.getId());

        repository.save(replacement);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(venue.getId())).get().extracting(Venue::getName).isEqualTo("A2");
    }

    @Test
    void findById_unknownOrNull_returnsEmpty() {
        assertThat(repository.findById(42L)).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    void deleteById_removesEntity() {
        Venue venue = repository.save(new Venue("A", "addr", "Kyiv"));

        repository.deleteById(venue.getId());

        assertThat(repository.existsById(venue.getId())).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAll_returnsEntitiesOrderedById() {
        repository.save(new Venue("A", "addr", "Kyiv"));
        repository.save(new Venue("B", "addr", "Lviv"));
        repository.save(new Venue("C", "addr", "Odesa"));

        assertThat(repository.findAll()).extracting(Venue::getName).containsExactly("A", "B", "C");
    }

    @Test
    void findByCity_isCaseInsensitive() {
        repository.save(new Venue("A", "addr", "Kyiv"));
        repository.save(new Venue("B", "addr", "Lviv"));

        assertThat(repository.findByCity("kyiv")).extracting(Venue::getName).containsExactly("A");
    }
}
