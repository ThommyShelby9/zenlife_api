package com.api.expo.seeders;

import com.api.expo.models.PositiveThought;
import com.api.expo.repository.PositiveThoughtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PositiveThoughtSeeder implements CommandLineRunner {

    private final PositiveThoughtRepository positiveThoughtRepository;

    @Override
    public void run(String... args) {
        // Vérifier si des données existent déjà
        if (positiveThoughtRepository.count() == 0) {
            seedPositiveThoughts();
            System.out.println("✅ 50 pensées positives ont été ajoutées à la base de données");
        } else {
            System.out.println("⏩ Le seeding des pensées positives a été ignoré car des données existent déjà");
        }
    }

    private void seedPositiveThoughts() {
        Map<String, List<String>> thoughtsByCategory = new HashMap<>();

        // Motivation
        List<String> motivationThoughts = new ArrayList<>();
        motivationThoughts.add("🚀 Chaque effort que vous faites aujourd'hui vous rapproche un peu plus de vos objectifs.");
        motivationThoughts.add("💪 Le succès n'est pas final, l'échec n'est pas fatal : c'est le courage de continuer qui compte.");
        motivationThoughts.add("⚡ La motivation vous fait démarrer, l'habitude vous fait continuer.");
        motivationThoughts.add("🔥 Votre seule limite est celle que vous vous fixez dans votre esprit.");
        motivationThoughts.add("🏃‍♂️ N'attendez pas d'être prêt pour commencer, commencez pour être prêt.");
        motivationThoughts.add("🌱 Le meilleur moment pour planter un arbre était il y a 20 ans. Le deuxième meilleur moment est maintenant.");
        thoughtsByCategory.put("Motivation", motivationThoughts);

        // Bonheur
        List<String> bonheurThoughts = new ArrayList<>();
        bonheurThoughts.add("😊 Le bonheur n'est pas une destination, c'est une façon de voyager.");
        bonheurThoughts.add("🌈 La joie que nous donnons aux autres est la joie qui nous revient.");
        bonheurThoughts.add("✨ Soyez heureux avec ce que vous avez pendant que vous poursuivez ce que vous voulez.");
        bonheurThoughts.add("🌞 Le bonheur est une décision quotidienne, pas une destination lointaine.");
        bonheurThoughts.add("😄 La vie est plus belle quand on la regarde avec des yeux pleins d'espoir.");
        bonheurThoughts.add("🦋 Faites plus de ce qui vous rend heureux.");
        thoughtsByCategory.put("Bonheur", bonheurThoughts);

        // Succès
        List<String> succesThoughts = new ArrayList<>();
        succesThoughts.add("🏆 Le succès, c'est tomber sept fois et se relever huit.");
        succesThoughts.add("💯 Votre réussite sera le produit de votre attitude et de vos efforts quotidiens.");
        succesThoughts.add("👑 Le succès n'est pas la clé du bonheur. Le bonheur est la clé du succès.");
        succesThoughts.add("🚧 Le chemin vers le succès est toujours en construction.");
        succesThoughts.add("⭐ Derrière chaque succès se cache des heures d'efforts silencieux.");
        succesThoughts.add("🎯 Le succès est la somme de petits efforts répétés jour après jour.");
        thoughtsByCategory.put("Succès", succesThoughts);

        // Amour
        List<String> amourThoughts = new ArrayList<>();
        amourThoughts.add("❤️ L'amour est la seule chose qui grandit lorsqu'on la partage.");
        amourThoughts.add("💕 Aimer, ce n'est pas se regarder l'un l'autre, c'est regarder ensemble dans la même direction.");
        amourThoughts.add("💞 L'amour ne consiste pas à trouver quelqu'un avec qui vivre, mais à trouver celui sans qui on ne peut pas vivre.");
        amourThoughts.add("🥰 Le plus grand bonheur de la vie est d'être convaincu qu'on est aimé.");
        amourThoughts.add("💓 L'amour est une toile tissée par la patience et la compréhension.");
        amourThoughts.add("💖 L'amour ne se voit pas avec les yeux mais avec le cœur.");
        thoughtsByCategory.put("Amour", amourThoughts);

        // Amitié
        List<String> amitieThoughts = new ArrayList<>();
        amitieThoughts.add("🤝 Un ami, c'est une personne avec laquelle on ose être soi-même.");
        amitieThoughts.add("👫 L'amitié double les joies et divise les peines.");
        amitieThoughts.add("🫂 Un véritable ami est celui qui vous prend par la main et vous touche le cœur.");
        amitieThoughts.add("🤗 L'amitié est née le jour où quelqu'un a dit à un autre : 'Quoi ? Toi aussi ? Je croyais être le seul.'");
        amitieThoughts.add("👭 Les amis sont la famille que l'on choisit.");
        thoughtsByCategory.put("Amitié", amitieThoughts);

        // Santé
        List<String> santeThoughts = new ArrayList<>();
        santeThoughts.add("💪 Prenez soin de votre corps, c'est le seul endroit où vous êtes obligé de vivre.");
        santeThoughts.add("🍎 La santé est la richesse réelle, pas les pièces d'or et d'argent.");
        santeThoughts.add("🧘‍♀️ Votre corps est votre temple. Gardez-le pur et propre pour que l'âme puisse y résider.");
        santeThoughts.add("🌿 Une bonne santé n'est pas quelque chose que nous pouvons acheter. Cependant, c'est quelque chose que nous pouvons épargner.");
        santeThoughts.add("🌱 La santé physique et mentale sont les deux ailes qui permettent à l'humain de voler.");
        thoughtsByCategory.put("Santé", santeThoughts);

        // Sagesse
        List<String> sagesseThoughts = new ArrayList<>();
        sagesseThoughts.add("🧠 La sagesse commence dans l'émerveillement.");
        sagesseThoughts.add("📚 La patience est la compagne de la sagesse.");
        sagesseThoughts.add("🔍 La sagesse n'est pas le produit de la scolarité, mais de la tentative tout au long de la vie de l'acquérir.");
        sagesseThoughts.add("🧐 Connaître les autres est intelligence; se connaître soi-même est vraie sagesse.");
        sagesseThoughts.add("💭 Plus je sais, plus je sais que je ne sais pas.");
        sagesseThoughts.add("🦉 La sagesse est de voir le miraculeux dans le commun.");
        thoughtsByCategory.put("Sagesse", sagesseThoughts);

        // Méditation
        List<String> meditationThoughts = new ArrayList<>();
        meditationThoughts.add("🧘‍♂️ La méditation est le seul moyen de faire taire ce bavardage constant dans votre tête.");
        meditationThoughts.add("💆‍♀️ Votre esprit est comme l'eau ; quand il est agité, il est difficile de voir. Mais si vous le laissez s'installer, la réponse devient claire.");
        meditationThoughts.add("🕊️ Le silence est une source de grande force.");
        meditationThoughts.add("🙏 Le secret de la méditation est de devenir conscient sans être attaché.");
        meditationThoughts.add("🌀 Dans la tranquillité, l'esprit trouve la clarté.");
        thoughtsByCategory.put("Méditation", meditationThoughts);

        // Gratitude
        List<String> gratitudeThoughts = new ArrayList<>();
        gratitudeThoughts.add("🙏 La gratitude transforme ce que nous avons en suffisance.");
        gratitudeThoughts.add("💝 Commencez chaque jour avec un cœur reconnaissant.");
        gratitudeThoughts.add("🎁 La gratitude est non seulement la plus grande des vertus, mais la mère de toutes les autres.");
        gratitudeThoughts.add("✨ Quand nous nous concentrons sur notre gratitude, la marée de la déception s'inverse et l'abondance arrive.");
        gratitudeThoughts.add("❤️ Être reconnaissant, c'est reconnaître la valeur des choses que l'on reçoit et non pas les tenir pour acquises.");
        gratitudeThoughts.add("🥹 La gratitude est la mémoire du cœur.");
        thoughtsByCategory.put("Gratitude", gratitudeThoughts);

        // Auteurs possibles pour les pensées
        List<String> authors = List.of(
            "Albert Einstein", 
            "Gandhi", 
            "Confucius", 
            "Viktor Frankl",
            "Nelson Mandela", 
            "Bouddha", 
            "Lao Tseu",
            "Socrate",
            "Marie Curie",
            "Marc Aurèle",
            "Antoine de Saint-Exupéry",
            "Paulo Coelho",
            "Anonyme",
            "Dalai Lama",
            "Sénèque"
        );

        List<PositiveThought> allThoughts = new ArrayList<>();
        
        // Créer les pensées à partir de toutes les catégories
        thoughtsByCategory.forEach((category, thoughts) -> {
            for (String content : thoughts) {
                PositiveThought thought = new PositiveThought();
                thought.setContent(content);
                thought.setCategory(category);
                
                // Choix aléatoire d'un auteur (20% de chance d'être anonyme)
                if (Math.random() < 0.2) {
                    thought.setAuthor("Anonyme");
                } else {
                    int authorIndex = (int) (Math.random() * authors.size());
                    thought.setAuthor(authors.get(authorIndex));
                }
                
                thought.setCreatedAt(Instant.now());
                allThoughts.add(thought);
            }
        });
        
        // Ajouter quelques pensées supplémentaires si nécessaire pour atteindre 50
        while (allThoughts.size() < 50) {
            // Choisir une catégorie aléatoire
            String[] categories = thoughtsByCategory.keySet().toArray(new String[0]);
            String randomCategory = categories[(int) (Math.random() * categories.length)];
            
            PositiveThought thought = new PositiveThought();
            thought.setContent("✨ La vie est faite de petits bonheurs quotidiens, soyez attentif à les reconnaître.");
            thought.setCategory(randomCategory);
            thought.setAuthor("Anonyme");
            thought.setCreatedAt(Instant.now());
            allThoughts.add(thought);
        }
        
        // Enregistrer toutes les pensées
        positiveThoughtRepository.saveAll(allThoughts);
    }
}