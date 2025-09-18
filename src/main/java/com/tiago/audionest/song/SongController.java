package com.tiago.audionest.song;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tiago.audionest.storage.StorageService;

@RestController
@RequestMapping("/api")
public class SongController {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private StorageService storageService;

    @GetMapping("/songs")
    public List<Song> list() {
        return songRepository.findAll();
    }

    @PostMapping("/songs/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist) throws IOException {
        String filename = storageService.store(file);
        Song s = new Song();
        s.setFilename(filename);
        s.setTitle(title == null ? file.getOriginalFilename() : title);
        s.setArtist(artist);
        songRepository.save(s);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/songs/{id}/stream")
    public ResponseEntity<ResourceRegion> stream(@PathVariable Long id, @RequestHeader HttpHeaders headers)
            throws IOException {
        Optional<Song> o = songRepository.findById(id);
        if (o.isEmpty())
            return ResponseEntity.notFound().build();
        Song song = o.get();
        Resource resource = storageService.loadAsResource(song.getFilename());
        long contentLength = resource.contentLength();

        List<HttpRange> ranges = headers.getRange();
        ResourceRegion region;
        if (ranges == null || ranges.isEmpty()) {
            long regionLength = Math.min(1_000_000, contentLength);
            region = new ResourceRegion(resource, 0, regionLength);
        } else {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1_000_000, end - start + 1);
            region = new ResourceRegion(resource, start, rangeLength);
        }

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentLength(region.getCount());
        respHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        respHeaders.setContentType(
                MediaTypeFactory.getMediaType(song.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM));
        return new ResponseEntity<>(region, respHeaders, HttpStatus.PARTIAL_CONTENT);
    }
}
