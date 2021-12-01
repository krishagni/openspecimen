
class HomePageService {
  pageCards = [];

  sorted = false;

  registerCards(cards) {
    cards.forEach(card => this.registerCard(card));
  }

  registerCard(card) {
    this.pageCards.push(card);
  }

  getCards() {
    if (this.sorted || this.pageCards.length == 0) {
      return this.pageCards;
    }
    
    this.pageCards.sort((c1, c2) => c1.title.localeCompare(c2.title));
    this.sorted = true;
    return this.pageCards;
  }
}

export default new HomePageService();